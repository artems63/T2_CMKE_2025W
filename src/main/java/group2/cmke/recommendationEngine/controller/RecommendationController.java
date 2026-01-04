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


@RestController
@RequestMapping("/api")
public class RecommendationController {

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

  // Sends the GET request to the Wiener Linien API
  // with our closest stop as input and with fallback to coordinates.
  private String fetchTripXml(
      double userLat, double userLon,
      String destinationDiva,
      double destinationLon, double destinationLat
  ) {
    try {

      HaltestellenService.Stop originStop =
          haltestellenService.findClosestStop(userLat, userLon);
      String originDiva = originStop.diva;

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

      HttpResponse<String> divaResponse = client.send(
          HttpRequest.newBuilder(divaUri).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );

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

      HttpResponse<String> coordResponse = client.send(
          HttpRequest.newBuilder(coordUri).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );

      return coordResponse.body();

    } catch (Exception e) {
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

    var session = kieContainer.newKieSession();
    try {
      session.insert(fact);

      int fired = session.fireAllRules();
      System.out.println("Drools fired rules: " + fired);

    } finally {
      session.dispose();
    }

    RecommendationResponseDTO resp = new RecommendationResponseDTO();
    resp.setEnvironmental_factor(context.getEnvironmental_factor());
    resp.setConfidence_score(fact.getConfidenceScore());

    if (fact.getRecommended() != null) {
      resp.setRecommended_transport(fact.getRecommended().name());
    } else {
      resp.setRecommended_transport("UNKNOWN");
    }

    resp.setReason(fact.getReasons());
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

    return parseTransportModesFromXml(xml);
  }

  private static double clamp01(double x) {
    return Math.max(0.0, Math.min(1.0, x));
  }

  private static double round2(double x) {
    return Math.round(x * 100.0) / 100.0;
  }

  private double computeEnvironmentalFactor(ContextDTO ctx, UserPreferencesDTO pref) {
    // 1) Walk support: best for short distances, requires good weather
    double walkSupport = 0.0;
    if (pref.weather_ok) {
      // 0m -> 1.0, 2000m -> 0.0
      walkSupport = clamp01(1.0 - (ctx.distance_to_destination_meters / 2000.0));
    }

    // 2) Bike sharing support: needs good weather + bikes + not too far from station
    double bikeShareSupport = 0.0;
    if (pref.weather_ok
            && ctx.bikesAvailableAtStation
            && ctx.distance_to_next_bikesharing_station_meters >= 0) {
      // 0m -> 1.0, 800m -> 0.0
      bikeShareSupport = clamp01(1.0 - (ctx.distance_to_next_bikesharing_station_meters / 800.0));
    }

    // 3) Public transport support: requires route present + station distance
    double ptSupport = 0.0;
    boolean ptRouteExists = (ctx.public_transport_best_option != null && !ctx.public_transport_best_option.isEmpty());
    boolean ptAllowed = pref.has_public_transport_ticket || pref.is_open_to_buy_ticket;

    if (ptRouteExists && ptAllowed && ctx.distance_to_next_public_transport_station_meters >= 0) {
      // 0m -> 1.0, 1200m -> 0.0
      double nearStop = clamp01(1.0 - (ctx.distance_to_next_public_transport_station_meters / 1200.0));
      // PT is "eco-friendly", but not as perfect as walking -> cap it
      ptSupport = 0.75 * nearStop;
    } else if (ptRouteExists && !ptAllowed) {
      // PT exists but blocked by ticket constraint -> small support
      ptSupport = 0.05;
    }

    // 4) Car fallback: if only car is feasible, environmental factor should be low
    // We don't know "feasible" here perfectly, so treat ownership as fallback signal.
    double carFallback = 0.0;
    if (pref.owns_electric_car) carFallback = 0.35;
    else if (pref.owns_gas_car) carFallback = 0.15;

    // Combine: emphasize low-impact supports, reduce if only car is plausible.
    // We use a weighted max-like mixture: supports dominate; car fallback pulls down.
    double ecoSupport = 0.45 * walkSupport
            + 0.30 * bikeShareSupport
            + 0.25 * ptSupport;

    // If ecoSupport is very low, allow car fallback to be the only remaining "support",
    // but keep overall factor low.
    double factor = Math.max(ecoSupport, carFallback);

    // Sustainability preference is not a magic boost; it should slightly increase sensitivity
    // to eco options rather than add a constant.
    if (pref.environmentally_sustainable) {
      factor = clamp01(factor * 1.05);
    }

    return round2(factor);
  }

  private ContextDTO buildBaseContext(
      double distanceMeters,
      List<TransportMode> transportModes, UserPreferencesDTO userPreferences
  ) {
    ContextDTO context = new ContextDTO();
    context.distance_to_destination_meters = distanceMeters;
    context.walking_ok = distanceMeters <= 2000;
    context.public_transport_best_option = transportModes;

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

    BikeSharingService.BikeStation bikeStation =
        bikeSharingService.findClosestStationWithBikes(
            userPreferences.lat,
            userPreferences.lon
        );

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

    double distanceMeters = calculateDistanceToDestination(userPreferences);

    List<TransportMode> transportModes =
        fetchAndParseTransportModes(userPreferences);

    ContextDTO context =
        buildBaseContext(distanceMeters, transportModes, userPreferences);

    HaltestellenService.Stop originStop =
        enrichContextWithPublicTransportDistances(userPreferences, context);

    BikeSharingService.BikeStation bikeStation =
        enrichContextWithBikeSharing(userPreferences, context);

    context.environmental_factor = computeEnvironmentalFactor(context, userPreferences);

    RecommendationResponseDTO response =
        runDrools(context, userPreferences);

    logDebugInfo(
        userPreferences,
        context,
        transportModes,
        originStop,
        bikeStation,
        distanceMeters
    );

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