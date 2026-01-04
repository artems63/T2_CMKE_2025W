package group2.cmke.recommendationEngine.dto;

import group2.cmke.recommendationEngine.model.TransportMode;
import java.util.List;

// This DTO represents the context part of the JSON input file for our drools rules engine.
// This information is calculated inside the RecommendationController.java file.
public class ContextDTO {
  public double distance_to_destination_meters;
  public boolean walking_ok;
  public List<TransportMode> public_transport_best_option;
  public double distance_to_next_public_transport_station_meters;
  public double distance_to_next_bikesharing_station_meters;
  public boolean bikesAvailableAtStation;
  public double environmental_factor;

  public double getDistance_to_destination_meters() {
    return distance_to_destination_meters;
  }

  public boolean isWalking_ok() {
    return walking_ok;
  }

  public void setWalking_ok(boolean walking_ok) {
    this.walking_ok = walking_ok;
  }

  public List<TransportMode> getPublic_transport_best_option() {
    return public_transport_best_option;
  }

  public void setPublic_transport_best_option(
      List<TransportMode> public_transport_best_option) {
    this.public_transport_best_option = public_transport_best_option;
  }

  public double getDistance_to_next_public_transport_station_meters() {
    return distance_to_next_public_transport_station_meters;
  }

  public void setDistance_to_next_public_transport_station_meters(
      int distance_to_next_public_transport_station_meters) {
    this.distance_to_next_public_transport_station_meters = distance_to_next_public_transport_station_meters;
  }

  public double getDistance_to_next_bikesharing_station_meters() {
    return distance_to_next_bikesharing_station_meters;
  }

  public void setDistance_to_next_bikesharing_station_meters(
      int distance_to_next_bikesharing_station_meters) {
    this.distance_to_next_bikesharing_station_meters = distance_to_next_bikesharing_station_meters;
  }

  public double getEnvironmental_factor() {
    return environmental_factor;
  }

  public void setEnvironmental_factor(double environmental_factor) {
    this.environmental_factor = environmental_factor;
  }

  public void setDistance_to_destination_meters(double distance_to_destination_meters) {
    this.distance_to_destination_meters = distance_to_destination_meters;
  }

  public void setDistance_to_next_public_transport_station_meters(
      double distance_to_next_public_transport_station_meters) {
    this.distance_to_next_public_transport_station_meters = distance_to_next_public_transport_station_meters;
  }

  public void setDistance_to_next_bikesharing_station_meters(
      double distance_to_next_bikesharing_station_meters) {
    this.distance_to_next_bikesharing_station_meters = distance_to_next_bikesharing_station_meters;
  }

  public boolean isBikesAvailableAtStation() {
    return bikesAvailableAtStation;
  }

  public void setBikesAvailableAtStation(boolean bikesAvailableAtStation) {
    this.bikesAvailableAtStation = bikesAvailableAtStation;
  }

  @Override
  public String toString() {
    return "ContextDTO{" +
        "distance_to_destination_meters=" + distance_to_destination_meters +
        ", walking_ok=" + walking_ok +
        ", public_transport_best_option=" + public_transport_best_option +
        ", distance_to_next_public_transport_station_meters="
        + distance_to_next_public_transport_station_meters +
        ", distance_to_next_bikesharing_station_meters="
        + distance_to_next_bikesharing_station_meters
        +
        ", bikesAvailableAtStation=" + bikesAvailableAtStation +
        ", environmental_factor=" + environmental_factor +
        '}';
  }
}