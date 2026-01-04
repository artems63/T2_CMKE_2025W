package group2.cmke.recommendationEngine.controller;

import group2.cmke.recommendationEngine.dto.ContextDTO;
import group2.cmke.recommendationEngine.dto.RecommendationResponseDTO;
import group2.cmke.recommendationEngine.dto.UserPreferencesDTO;
import group2.cmke.recommendationEngine.model.TransportMode;
import group2.cmke.recommendationEngine.service.BikeSharingService;
import group2.cmke.recommendationEngine.service.HaltestellenService;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import group2.cmke.recommendationEngine.drools.Fact;
import org.kie.api.runtime.KieContainer;
import group2.cmke.recommendationEngine.prolog.EnvironmentalScoreService;



@RestController
@RequestMapping("/api")
public class RecommendationController {

  private static final String LOG_PATH = System.getenv("DEBUG_LOG_PATH") != null ? System.getenv("DEBUG_LOG_PATH") : (System.getProperty("user.dir") + java.io.File.separator + ".cursor" + java.io.File.separator + "debug.log");

  private double distanceToNextBikeStationMeters;
  private boolean bikesAvailableAtStation;

  // Stop ID of the first stop for the desired route. It's set to -1 if no stop exists or the API
  // does not work cause of the Wiener Linien or in offline mode.
  private Integer firstBoardingStopId;

  @Autowired
  private HaltestellenService haltestellenService;

  @Autowired
  private KieContainer kieContainer;

  @Autowired
  private BikeSharingService bikeSharingService;

  @Autowired
  private EnvironmentalScoreService environmentalScoreService;

  // Sends the GET request to the Wiener Linien API
  // with our closest stop as input and with fallback to coordinates.
  private String fetchTripXml(
      double userLat, double userLon,
      String destinationDiva,
      double destinationLon, double destinationLat
  ) {
    try {
      // #region agent log
      long findStopStart = System.currentTimeMillis();
      // #endregion
      HaltestellenService.Stop originStop =
          haltestellenService.findClosestStop(userLat, userLon);
      String originDiva = originStop.diva;
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"RecommendationController.fetchTripXml:74\",\"message\":\"Find closest stop completed\",\"data\":{\"durationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - findStopStart, System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion

      HttpClient client = HttpClient.newHttpClient();

      // Request with DIVA number.
      URI divaUri = UriComponentsBuilder
          .fromUriString("https://www.wienerlinien.at/ogd_routing/XML_TRIP_REQUEST2")
          .queryParam("type_origin", "diva")
          .queryParam("name_origin", originDiva)
          .queryParam("type_destination", "diva")
          .queryParam("name_destination", destinationDiva)
          .queryParam("language", "de")
          .build()
          .toUri();

      // #region agent log
      long apiCallStart = System.currentTimeMillis();
      // #endregion
      HttpResponse<String> divaResponse = client.send(
          HttpRequest.newBuilder(divaUri).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"RecommendationController.fetchTripXml:90\",\"message\":\"Wiener Linien DIVA API call completed\",\"data\":{\"durationMs\":%d,\"statusCode\":%d,\"responseLength\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - apiCallStart, divaResponse.statusCode(), divaResponse.body().length(), System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion

      String divaBody = divaResponse.body();

      // Check if the response is valid.
      if (!divaBody.contains("<itdItinerary/>")) {
        return divaBody; // success
      }

      // Fallback to coordinates if DIVA request does not return a valid response.
      String destCoord = destinationLat + ":" + destinationLon + ":WGS84";

      URI coordUri = UriComponentsBuilder
          .fromUriString("https://www.wienerlinien.at/ogd_routing/XML_TRIP_REQUEST2")
          .queryParam("type_origin", "diva")
          .queryParam("name_origin", originDiva)
          .queryParam("type_destination", "coord")
          .queryParam("name_destination", destCoord)
          .queryParam("language", "de")
          .build()
          .toUri();

      // #region agent log
      apiCallStart = System.currentTimeMillis();
      // #endregion
      HttpResponse<String> coordResponse = client.send(
          HttpRequest.newBuilder(coordUri).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"RecommendationController.fetchTripXml:115\",\"message\":\"Wiener Linien coord API call completed (fallback)\",\"data\":{\"durationMs\":%d,\"statusCode\":%d,\"responseLength\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - apiCallStart, coordResponse.statusCode(), coordResponse.body().length(), System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion

      return coordResponse.body();

    } catch (Exception e) {
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"RecommendationController.fetchTripXml:122\",\"message\":\"API call exception\",\"data\":{\"exception\":\"%s\"},\"timestamp\":%d}\n", e.getClass().getName() + ": " + e.getMessage(), System.currentTimeMillis()));
          fw.close();
      } catch (Exception ex) {}
      // #endregion
      // If the API is down or we have no internet connection we proceed with no public transport.
      return "<itdRequest></itdRequest>";
    }
  }

  // Calculates the distance between user location and user destination in meters.
  private double calculateDistanceToDestination(UserPreferencesDTO userPreferences) {
    return calculateDistance(
        userPreferences.lat,
        userPreferences.lon,
        userPreferences.destination_lat,
        userPreferences.destination_lon
    );
  }

  private RecommendationResponseDTO runDrools(ContextDTO context, UserPreferencesDTO preferences) {
    Fact fact = new Fact(context, preferences);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"RecommendationController.runDrools:174\",\"message\":\"Before Drools - ownership values\",\"data\":{\"owns_non_electric_transport\":%s,\"owns_electric_micro_mobility\":%s,\"bikeDisqualifiedBefore\":%s,\"eBikeDisqualifiedBefore\":%s},\"timestamp\":%d}\n", preferences.owns_non_electric_transport, preferences.owns_electric_micro_mobility, fact.isDisqualified(Fact.Mode.BIKE), fact.isDisqualified(Fact.Mode.E_BIKE), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"RecommendationController.runDrools:137\",\"message\":\"Before Drools - confidence score\",\"data\":{\"confidenceScore\":%f},\"timestamp\":%d}\n", fact.getConfidenceScore(), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    var session = kieContainer.newKieSession();
    try {
      session.insert(fact);

      int fired = session.fireAllRules();
      System.out.println("Drools fired rules: " + fired);
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"RecommendationController.runDrools:188\",\"message\":\"After Drools - disqualification status\",\"data\":{\"bikeDisqualified\":%s,\"eBikeDisqualified\":%s,\"bikeScore\":%d,\"eBikeScore\":%d,\"recommended\":\"%s\"},\"timestamp\":%d}\n", fact.isDisqualified(Fact.Mode.BIKE), fact.isDisqualified(Fact.Mode.E_BIKE), fact.getScore(Fact.Mode.BIKE), fact.getScore(Fact.Mode.E_BIKE), (fact.getRecommended() != null ? fact.getRecommended().name() : "null"), System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"RecommendationController.runDrools:144\",\"message\":\"After Drools fireAllRules\",\"data\":{\"fired\":%d,\"recommended\":\"%s\",\"confidenceScore\":%f},\"timestamp\":%d}\n", fired, (fact.getRecommended() != null ? fact.getRecommended().name() : "null"), fact.getConfidenceScore(), System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion

    } finally {
      session.dispose();
    }

    RecommendationResponseDTO resp = new RecommendationResponseDTO();
    resp.setConfidence_score(fact.getConfidenceScore());
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\",\"location\":\"RecommendationController.runDrools:166\",\"message\":\"Before setting recommended_transport\",\"data\":{\"factRecommendedIsNull\":%s,\"factRecommended\":\"%s\"},\"timestamp\":%d}\n", (fact.getRecommended() == null), (fact.getRecommended() != null ? fact.getRecommended().name() : "null"), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    if (fact.getRecommended() != null) {
      resp.setRecommended_transport(fact.getRecommended().name());
      // Calculate mode-specific environmental factor
      double modeEnvFactor = environmentalScoreService.modeEnvironmentalFactor(
          fact.getRecommended().name(),
          context.distance_to_destination_meters,
          preferences.weather_ok
      );
      resp.setEnvironmental_factor(modeEnvFactor);
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\",\"location\":\"RecommendationController.runDrools:178\",\"message\":\"Set recommended_transport to mode name\",\"data\":{\"recommended_transport\":\"%s\",\"mode_env_factor\":%f},\"timestamp\":%d}\n", fact.getRecommended().name(), modeEnvFactor, System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion
    } else {
      resp.setRecommended_transport("UNKNOWN");
      resp.setEnvironmental_factor(context.getEnvironmental_factor());
      // #region agent log
      try {
          java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
          fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\",\"location\":\"RecommendationController.runDrools:180\",\"message\":\"Set recommended_transport to UNKNOWN\",\"data\":{},\"timestamp\":%d}\n", System.currentTimeMillis()));
          fw.close();
      } catch (Exception e) {}
      // #endregion
    }

    resp.setReason(fact.getReasons());
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"RecommendationController.runDrools:184\",\"message\":\"Final response DTO values\",\"data\":{\"environmental_factor\":%f,\"confidence_score\":%f,\"recommended_transport\":\"%s\"},\"timestamp\":%d}\n", resp.getEnvironmental_factor(), resp.getConfidence_score(), (resp.getRecommended_transport() != null ? resp.getRecommended_transport() : "null"), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion
    return resp;
  }

  // Calls the fetch method for our api call and then the response is parsed into a list of objects.
  private List<TransportMode> fetchAndParseTransportModes(
      UserPreferencesDTO userPreferences
  ) throws Exception {

    String xml = fetchTripXml(
        userPreferences.lat,
        userPreferences.lon,
        userPreferences.destination_diva,
        userPreferences.destination_lon,
        userPreferences.destination_lat
    );

    // #region agent log
    long parseStart = System.currentTimeMillis();
    // #endregion
    List<TransportMode> result = parseTransportModesFromXml(xml);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\",\"location\":\"RecommendationController.fetchAndParseTransportModes:221\",\"message\":\"Parse XML completed\",\"data\":{\"durationMs\":%d,\"xmlLength\":%d,\"resultCount\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - parseStart, xml.length(), result.size(), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion
    return result;
  }

  private static double clamp01(double x) {
    return Math.max(0.0, Math.min(1.0, x));
  }

  private static double round2(double x) {
    return Math.round(x * 100.0) / 100.0;
  }

  private ContextDTO buildBaseContext(
      double distanceMeters,
      List<TransportMode> transportModes, UserPreferencesDTO userPreferences
  ) {
    ContextDTO context = new ContextDTO();
    context.distance_to_destination_meters = distanceMeters;
    context.walking_ok = distanceMeters <= 2000;
    context.public_transport_best_option = transportModes;

    context.environmental_factor =
            environmentalScoreService.environmentalFactor(
                    distanceMeters,
                    userPreferences.weather_ok,
                    userPreferences.environmentally_sustainable
            );
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"RecommendationController.buildBaseContext:203\",\"message\":\"Context environmental_factor set\",\"data\":{\"environmental_factor\":%f},\"timestamp\":%d}\n", context.environmental_factor, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    return context;
  }

  // Calculates the public transport variables (distance to station) and adds them to the context.
  // Here we do not add just the next public station but we add the next relevant public station
  // for the given route.
  private HaltestellenService.Stop enrichContextWithPublicTransportDistances(
      UserPreferencesDTO userPreferences,
      ContextDTO context
  ) {

    HaltestellenService.Stop originStop =
        haltestellenService.findClosestStop(
            userPreferences.lat,
            userPreferences.lon
        );

    HaltestellenService.Stop originStopForDesiredRoute =
        haltestellenService.findStopByStopId(firstBoardingStopId);

    if (originStopForDesiredRoute != null) {
      context.distance_to_next_public_transport_station_meters =
          calculateDistance(
              userPreferences.lat,
              userPreferences.lon,
              originStopForDesiredRoute.lat,
              originStopForDesiredRoute.lon
          );
    } else {
      context.distance_to_next_public_transport_station_meters = -1;
    }

    return originStop;
  }

  // Calculates the bike sharing variables (amount, distance to station) and adds them to the context.
  private BikeSharingService.BikeStation enrichContextWithBikeSharing(
      UserPreferencesDTO userPreferences,
      ContextDTO context
  ) {
    // #region agent log
    long findStationStart = System.currentTimeMillis();
    // #endregion
    BikeSharingService.BikeStation bikeStation =
        bikeSharingService.findClosestStationWithBikes(
            userPreferences.lat,
            userPreferences.lon
        );
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"RecommendationController.enrichContextWithBikeSharing:296\",\"message\":\"Find closest bike station completed\",\"data\":{\"durationMs\":%d,\"stationFound\":%s},\"timestamp\":%d}\n", System.currentTimeMillis() - findStationStart, (bikeStation != null), System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    if (bikeStation != null) {
      distanceToNextBikeStationMeters =
          calculateDistance(
              userPreferences.lat,
              userPreferences.lon,
              bikeStation.lat,
              bikeStation.lon
          );

      bikesAvailableAtStation = bikeStation.bikesAvailable > 0;
    } else {
      distanceToNextBikeStationMeters = -1;
      bikesAvailableAtStation = false;
    }

    context.distance_to_next_bikesharing_station_meters = distanceToNextBikeStationMeters;
    context.bikesAvailableAtStation = bikesAvailableAtStation;

    return bikeStation;
  }

  // Logs and prints the calculated values for debugging purposes.
  private void logDebugInfo(
      UserPreferencesDTO userPreferences,
      ContextDTO context,
      List<TransportMode> transportModes,
      HaltestellenService.Stop originStop,
      BikeSharingService.BikeStation bikeStation,
      double distanceMeters
  ) {
    System.out.println("Weather is ok?: " + userPreferences.weather_ok);
    System.out.println("Distance to destination (m): " + distanceMeters);
    System.out.println(transportModes + " are the best Transport modes for given route!");
    System.out.println("----------------------------------------------------");
    System.out.println("First boarding stop ID: " + firstBoardingStopId);
    System.out.println(userPreferences.lat + " Lat of User");
    System.out.println(userPreferences.lon + "Lon of User");
    System.out.println(originStop.lat + " Lat of closest stop");
    System.out.println(originStop.lon + " Lon of closest stop");
    System.out.println("----------------------------------------------------------------------");

    if (bikeStation != null) {
      System.out.println("Closest bike station: " + bikeStation.name);
      System.out.println("Distance: " + distanceToNextBikeStationMeters);
      System.out.println("Bikes available: " + bikeStation.bikesAvailable);
    }

    System.out.println("----------------------------------------------------------------------");
    System.out.println(context);
    System.out.println(userPreferences);
  }

  // Main HTTP method that calls all other methods to enrich the JSON files and call the drools engine.
  // Also notifies the user with the recommendation from the drools engine.
  @PostMapping("/recommend")
  public RecommendationResponseDTO recommend(@RequestBody UserPreferencesDTO userPreferences) throws Exception {
    // #region agent log
    long requestStartTime = System.currentTimeMillis();
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"ALL\",\"location\":\"RecommendationController.recommend:357\",\"message\":\"Request started\",\"data\":{\"lat\":%f,\"lon\":%f},\"timestamp\":%d}\n", userPreferences.lat, userPreferences.lon, requestStartTime));
        fw.close();
    } catch (Exception e) {}
    // #endregion
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"RecommendationController.recommend:420\",\"message\":\"User preferences received\",\"data\":{\"owns_non_electric_transport\":%s,\"owns_electric_micro_mobility\":%s,\"owns_e_bike\":%s,\"owns_gas_car\":%s,\"owns_electric_car\":%s},\"timestamp\":%d}\n", userPreferences.owns_non_electric_transport, userPreferences.owns_electric_micro_mobility, userPreferences.owns_e_bike, userPreferences.owns_gas_car, userPreferences.owns_electric_car, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    long stepStartTime = System.currentTimeMillis();
    double distanceMeters = calculateDistanceToDestination(userPreferences);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"ALL\",\"location\":\"RecommendationController.recommend:360\",\"message\":\"Distance calculation completed\",\"data\":{\"distanceMeters\":%f,\"durationMs\":%d},\"timestamp\":%d}\n", distanceMeters, System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    stepStartTime = System.currentTimeMillis();
    List<TransportMode> transportModes =
        fetchAndParseTransportModes(userPreferences);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"RecommendationController.recommend:362\",\"message\":\"Fetch and parse transport modes completed\",\"data\":{\"transportModesCount\":%d,\"durationMs\":%d},\"timestamp\":%d}\n", transportModes.size(), System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    stepStartTime = System.currentTimeMillis();
    ContextDTO context =
        buildBaseContext(distanceMeters, transportModes, userPreferences);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"RecommendationController.recommend:365\",\"message\":\"Build base context completed\",\"data\":{\"durationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    stepStartTime = System.currentTimeMillis();
    HaltestellenService.Stop originStop =
        enrichContextWithPublicTransportDistances(userPreferences, context);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"RecommendationController.recommend:368\",\"message\":\"Enrich with public transport distances completed\",\"data\":{\"durationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    stepStartTime = System.currentTimeMillis();
    BikeSharingService.BikeStation bikeStation =
        enrichContextWithBikeSharing(userPreferences, context);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"RecommendationController.recommend:371\",\"message\":\"Enrich with bike sharing completed\",\"data\":{\"durationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    stepStartTime = System.currentTimeMillis();
    RecommendationResponseDTO response =
        runDrools(context, userPreferences);
    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"RecommendationController.recommend:374\",\"message\":\"Run Drools completed\",\"data\":{\"durationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - stepStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    logDebugInfo(
        userPreferences,
        context,
        transportModes,
        originStop,
        bikeStation,
        distanceMeters
    );

    // #region agent log
    try {
        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"ALL\",\"location\":\"RecommendationController.recommend:385\",\"message\":\"Request completed\",\"data\":{\"totalDurationMs\":%d},\"timestamp\":%d}\n", System.currentTimeMillis() - requestStartTime, System.currentTimeMillis()));
        fw.close();
    } catch (Exception e) {}
    // #endregion

    return response;
  }

  // Helper method to calculate the distance in meters between to sets of coordinates
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371000; // Radius der Erde in Metern
    double f1 = Math.toRadians(lat1);
    double f2 = Math.toRadians(lat2);
    double Df = Math.toRadians(lat2 - lat1);
    double Dl = Math.toRadians(lon2 - lon1);
    double a = Math.sin(Df / 2) * Math.sin(Df / 2) +
        Math.cos(f1) * Math.cos(f2) *
            Math.sin(Dl / 2) * Math.sin(Dl / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  // Helper method that removes duplicates from our list of given transport modes for a certain route
  private List<TransportMode> removeDuplicates(List<TransportMode> input) {
    Map<String, TransportMode> unique = new LinkedHashMap<>();

    for (TransportMode t : input) {
      unique.put(t.getMode() + "_" + t.getLineName(), t);
    }

    return new ArrayList<>(unique.values());
  }

  // Accepts the Wiener Linien response HTML and parses the response into a List<TransportMode>
  private List<TransportMode> parseTransportModesFromXml(String xml)
      throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(
        new InputSource(new StringReader(xml))
    );

    XPath xpath = XPathFactory.newInstance().newXPath();

    String firstStopExpr =
        "/itdRequest/itdTripRequest/itdItinerary/" +
            "itdRouteList/itdRoute[1]/" +
            "itdPartialRouteList/itdPartialRoute[1]/" +
            "itdPoint[@usage='departure']";

    Element firstStopElement = (Element) xpath.evaluate(
        firstStopExpr,
        doc,
        XPathConstants.NODE
    );

    if (firstStopElement != null) {
      this.firstBoardingStopId =
          Integer.parseInt(firstStopElement.getAttribute("stopID"));
    } else {
      this.firstBoardingStopId = -1;
    }

    String expression =
        "/itdRequest/itdTripRequest/itdItinerary/" +
            "itdRouteList/itdRoute[1]//itdMeansOfTransport";

    NodeList motNodes = (NodeList) xpath.evaluate(
        expression,
        doc,
        XPathConstants.NODESET
    );

    List<TransportMode> result = new ArrayList<>();

    for (int i = 0; i < motNodes.getLength(); i++) {
      Element mot = (Element) motNodes.item(i);

      String mode = mot.getAttribute("productName");
      String line = mot.getAttribute("shortname");

      if (!mode.isBlank() && !line.isBlank()) {
        result.add(new TransportMode(mode, line));
      }
    }

    return removeDuplicates(result);
  }
}
