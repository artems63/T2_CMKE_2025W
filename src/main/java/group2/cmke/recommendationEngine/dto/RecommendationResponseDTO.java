package group2.cmke.recommendationEngine.dto;

import java.util.List;

public class RecommendationResponseDTO {
  public String recommended_transport;
  public double environmental_factor;
  public double confidence_score;
  public List<String> reason;
  private String decision_timestamp;
  private String destination;

  public String getDecisionTimestamp() {
    return decision_timestamp;
  }

  public void setDecisionTimestamp(String decision_timestamp) {
    this.decision_timestamp = decision_timestamp;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getRecommended_transport() {
    return recommended_transport;
  }

  public void setRecommended_transport(String recommended_transport) {
    this.recommended_transport = recommended_transport;
  }

  public double getEnvironmental_factor() {
    return environmental_factor;
  }

  public void setEnvironmental_factor(double environmental_factor) {
    this.environmental_factor = environmental_factor;
  }

  public double getConfidence_score() {
    return confidence_score;
  }

  public void setConfidence_score(double confidence_score) {
    this.confidence_score = confidence_score;
  }

  public List<String> getReason() {
    return reason;
  }

  public void setReason(List<String> reason) {
    this.reason = reason;
  }

  @Override
  public String toString() {
    return "RecommendationResponseDTO{" +
        "recommended_transport='" + recommended_transport + '\'' +
        ", environmental_factor=" + environmental_factor +
        ", confidence_score=" + confidence_score +
        ", reason=" + reason +
        '}';
  }
}

