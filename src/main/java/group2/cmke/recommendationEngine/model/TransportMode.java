package group2.cmke.recommendationEngine.model;

// This helper class is used to objectify the xml response from the Wiener Linien.
// Mode consists of transport modes like "Straßenbahn", "UBahn", "Train", "Bus".
public class TransportMode {
  private String mode;
  private String lineName;

  public TransportMode(String mode, String lineName) {
    this.mode = mode;
    this.lineName = lineName;
  }

  public String getMode() {
    return mode;
  }

  public String getLineName() {
    return lineName;
  }

  @Override
  public String toString() {
    if (mode == null || mode.isBlank()) {
      return "";
    }

    String normalizedMode = mode.trim().toLowerCase();

    String prettyMode = switch (normalizedMode) {
      case "u-bahn", "ubahn" -> "U-Bahn";
      case "s-bahn", "sbahn" -> "S-Bahn";
      default -> normalizedMode.substring(0, 1).toUpperCase() +
          normalizedMode.substring(1);
    };

    if (lineName == null || lineName.isBlank()) {
      return prettyMode;
    }

    return prettyMode + " " + lineName;
  }
}