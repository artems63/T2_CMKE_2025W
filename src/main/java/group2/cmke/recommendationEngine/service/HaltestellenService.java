package group2.cmke.recommendationEngine.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

// This service class extracts information from the Wiener Linien "Haltestellen.csv" file.
// This file has been cross-checked with the Wiener Linien "Fahrwegverläufe.csv" and therefore been filtered.
// It is used by the controller to calculate the distance to the closest station, to get the DIVA
// numbers for the Wiener Linien Routing API call and extract all the possible stops by ID.
@Service
public class HaltestellenService {

  public static class Stop {
    public String stopId;
    public String diva;
    public String stopText;
    public double lon;
    public double lat;

    public Stop(String stopId, String diva, String stopText, double lon, double lat) {
      this.stopId = stopId;
      this.diva = diva;
      this.stopText = stopText;
      this.lon = lon;
      this.lat = lat;
    }
  }

  private List<Stop> stops = new ArrayList<>();

  public HaltestellenService() {
    try {
      ClassPathResource resource = new ClassPathResource("static/haltestellen.csv");
      try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
        String line;
        br.readLine();

        int lineNumber = 1;
        while ((line = br.readLine()) != null) {
          lineNumber++;
          try {
            String[] cols = line.split(";");

            // column indexes of our coordinates from the csv file.
            double lon = Double.parseDouble(cols[5].trim());
            double lat = Double.parseDouble(cols[6].trim());

            stops.add(new Stop(
                cols[0].trim(),
                cols[1].trim(),
                cols[2].trim(),
                lon,
                lat
            ));
          } catch (Exception e) {
            System.err.println("Fehlerhafte Zeile " + lineNumber + ": " + line);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fehler beim Laden der Haltestellen", e);
    }
  }

  public Stop findClosestStop(double lat, double lon) {
    Stop closest = null;
    double minDist = Double.MAX_VALUE;

    for (Stop s : stops) {
      double dist = haversine(lat, lon, s.lat, s.lon);
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
    double φ1 = Math.toRadians(lat1);
    double φ2 = Math.toRadians(lat2);
    double Δφ = Math.toRadians(lat2 - lat1);
    double Δλ = Math.toRadians(lon2 - lon1);
    double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
        Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  public List<Stop> getAllStops() {
    return stops;
  }

  public Stop findStopByStopId(String stopId) {
    if (stopId == null) {
      return null;
    }

    for (Stop s : stops) {
      if (stopId.equals(s.diva)) {
        return s;
      }
    }

    return null;
  }

  public Stop findStopByStopId(int stopId) {
    return findStopByStopId(String.valueOf(stopId));
  }
}