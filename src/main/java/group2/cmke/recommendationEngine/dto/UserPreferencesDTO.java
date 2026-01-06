package group2.cmke.recommendationEngine.dto;

import java.util.List;

// This DTO represents the userpreferences part of the JSON input file for our drools rules engine.
// This information is given by the user himself from the frontend.
public class UserPreferencesDTO {
  public boolean environmentally_sustainable;

  // Legacy fields (keep for backward compatibility with existing rules/logic)
  public boolean owns_non_electric_transport;        // should mean: owns BIKE
  public boolean owns_electric_micro_mobility;       // should mean: owns E_BIKE or E_SCOOTER
  public boolean owns_e_bike;
  public boolean owns_gas_car;
  public boolean owns_electric_car;

  // NEW: explicit scooter ownership (fixes scooter being treated as bike)
  public boolean owns_scooter;
  public boolean owns_e_scooter;

  public boolean has_public_transport_ticket;
  public boolean is_open_to_buy_ticket;
  public boolean wants_bike_sharing;
  public boolean wants_public_transport;
  public boolean weather_ok;

  // public String preferred_transport_mode;
  public List<String> preferred_transport_modes;

  public double lat;
  public double lon;
  public String destination_diva;
  public String destination_text;
  public double destination_lat;
  public double destination_lon;

  public boolean isEnvironmentally_sustainable() {
    return environmentally_sustainable;
  }

  public void setEnvironmentally_sustainable(boolean environmentally_sustainable) {
    this.environmentally_sustainable = environmentally_sustainable;
  }

  public boolean isOwns_non_electric_transport() {
    return owns_non_electric_transport;
  }

  public void setOwns_non_electric_transport(boolean owns_non_electric_transport) {
    this.owns_non_electric_transport = owns_non_electric_transport;
  }

  public boolean isOwns_electric_micro_mobility() {
    return owns_electric_micro_mobility;
  }

  public void setOwns_electric_micro_mobility(boolean owns_electric_micro_mobility) {
    this.owns_electric_micro_mobility = owns_electric_micro_mobility;
  }

  public boolean isOwns_e_bike() {
    return owns_e_bike;
  }

  public void setOwns_e_bike(boolean owns_e_bike) {
    this.owns_e_bike = owns_e_bike;
  }

  public boolean isOwns_gas_car() {
    return owns_gas_car;
  }

  public void setOwns_gas_car(boolean owns_gas_car) {
    this.owns_gas_car = owns_gas_car;
  }

  public boolean isOwns_electric_car() {
    return owns_electric_car;
  }

  public void setOwns_electric_car(boolean owns_electric_car) {
    this.owns_electric_car = owns_electric_car;
  }

  // ---- NEW getters/setters for scooter ownership ----

  public boolean isOwns_scooter() {
    return owns_scooter;
  }

  public void setOwns_scooter(boolean owns_scooter) {
    this.owns_scooter = owns_scooter;
  }

  public boolean isOwns_e_scooter() {
    return owns_e_scooter;
  }

  public void setOwns_e_scooter(boolean owns_e_scooter) {
    this.owns_e_scooter = owns_e_scooter;
  }

  public boolean isHas_public_transport_ticket() {
    return has_public_transport_ticket;
  }

  public void setHas_public_transport_ticket(boolean has_public_transport_ticket) {
    this.has_public_transport_ticket = has_public_transport_ticket;
  }

  public boolean isIs_open_to_buy_ticket() {
    return is_open_to_buy_ticket;
  }

  public void setIs_open_to_buy_ticket(boolean is_open_to_buy_ticket) {
    this.is_open_to_buy_ticket = is_open_to_buy_ticket;
  }

  public boolean isWants_bike_sharing() {
    return wants_bike_sharing;
  }

  public void setWants_bike_sharing(boolean wants_bike_sharing) {
    this.wants_bike_sharing = wants_bike_sharing;
  }

  public boolean isWants_public_transport() {
    return wants_public_transport;
  }

  public void setWants_public_transport(boolean wants_public_transport) {
    this.wants_public_transport = wants_public_transport;
  }

  public boolean isWeather_ok() {
    return weather_ok;
  }

  public void setWeather_ok(boolean weather_ok) {
    this.weather_ok = weather_ok;
  }

  // public String getPreferred_transport_mode() {
  //   return preferred_transport_mode;
  // }
  //
  // public void setPreferred_transport_mode(String preferred_transport_mode) {
  //   this.preferred_transport_mode = preferred_transport_mode;
  // }

  public List<String> getPreferred_transport_modes() {
    return preferred_transport_modes;
  }

  public void setPreferred_transport_modes(List<String> preferred_transport_modes) {
    this.preferred_transport_modes = preferred_transport_modes;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public String getDestination_diva() {
    return destination_diva;
  }

  public void setDestination_diva(String destination_diva) {
    this.destination_diva = destination_diva;
  }

  public String getDestination_text() {
    return destination_text;
  }

  public void setDestination_text(String destination_text) {
    this.destination_text = destination_text;
  }

  public double getDestination_lat() {
    return destination_lat;
  }

  public void setDestination_lat(double destination_lat) {
    this.destination_lat = destination_lat;
  }

  public double getDestination_lon() {
    return destination_lon;
  }

  public void setDestination_lon(double destination_lon) {
    this.destination_lon = destination_lon;
  }

  @Override
  public String toString() {
    return "UserPreferencesDTO{" +
            "environmentally_sustainable=" + environmentally_sustainable +
            ", owns_non_electric_transport=" + owns_non_electric_transport +
            ", owns_electric_micro_mobility=" + owns_electric_micro_mobility +
            ", owns_e_bike=" + owns_e_bike +
            ", owns_gas_car=" + owns_gas_car +
            ", owns_electric_car=" + owns_electric_car +
            ", owns_scooter=" + owns_scooter +
            ", owns_e_scooter=" + owns_e_scooter +
            ", has_public_transport_ticket=" + has_public_transport_ticket +
            ", is_open_to_buy_ticket=" + is_open_to_buy_ticket +
            ", wants_bike_sharing=" + wants_bike_sharing +
            ", wants_public_transport=" + wants_public_transport +
            ", weather_ok=" + weather_ok +
            // ", preferred_transport_mode='" + preferred_transport_mode + '\'' +
            ", preferred_transport_modes=" + preferred_transport_modes +
            ", lat=" + lat +
            ", lon=" + lon +
            ", destination_diva='" + destination_diva + '\'' +
            ", destination_text='" + destination_text + '\'' +
            ", destination_lat=" + destination_lat +
            ", destination_lon=" + destination_lon +
            '}';
  }
}
