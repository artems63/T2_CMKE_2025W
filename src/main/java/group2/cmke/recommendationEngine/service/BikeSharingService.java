package group2.cmke.recommendationEngine.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// This service class extracts information from the Wien Mobil bike sharing JSON file.
// It is used by the controller to calculate the distance to the closest bike sharing station.
@Service
public class BikeSharingService {

  public static class BikeStation {
    public String stationId;
    public String name;
    public double lat;
    public double lon;
    public int capacity;
    public int bikesAvailable;
  }

  private final List<BikeStation> stations = new ArrayList<>();

  public BikeSharingService() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      InputStream is = new ClassPathResource(
          "static/station_information_bikes_wienmobil.json"
      ).getInputStream();

      JsonNode root = mapper.readTree(is);
      JsonNode stationArray = root.path("data").path("stations");

      for (JsonNode s : stationArray) {
        BikeStation bs = new BikeStation();
        bs.stationId = s.path("station_id").asText("");
        bs.name = s.path("name").asText("");
        bs.lat = s.path("lat").asDouble(0.0);
        bs.lon = s.path("lon").asDouble(0.0);
        bs.capacity = s.path("capacity").asInt(0);
        bs.bikesAvailable = bs.capacity;

        stations.add(bs);
      }
    } catch (Exception e) {
      throw new RuntimeException("Fehler beim Laden der Bikesharing-Daten", e);
    }
  }

  public BikeStation findClosestStationWithBikes(double userLat, double userLon) {
    BikeStation closest = null;
    double minDist = Double.MAX_VALUE;

    for (BikeStation s : stations) {
      if (s.bikesAvailable <= 0) {
        continue;
      }

      double dist = haversine(userLat, userLon, s.lat, s.lon);
      if (dist < minDist) {
        minDist = dist;
        closest = s;
      }
    }
    return closest;
  }

  // Helper method to calculate the distance between two sets of coordinates.
  private double haversine(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371000;
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
}
