package group2.cmke.recommendationEngine.drools;

import java.util.*;
import group2.cmke.recommendationEngine.dto.ContextDTO;
import group2.cmke.recommendationEngine.dto.UserPreferencesDTO;

public class Fact {

  public enum Mode { WALK, BIKE, E_BIKE, SCOOTER, E_SCOOTER, BIKESHARE, PUBLIC_TRANSPORT, GAS_CAR, ELECTRIC_CAR }

  private final ContextDTO context;
  private final UserPreferencesDTO preferences;

  private final EnumMap<Mode, Integer> score = new EnumMap<>(Mode.class);
  private final EnumMap<Mode, Boolean> disqualified = new EnumMap<>(Mode.class);

  private final List<String> reasons = new ArrayList<>();
  private final List<String> rulesTriggered = new ArrayList<>();

  private Mode recommended;

  private double confidenceScore;

  public Fact(ContextDTO context, UserPreferencesDTO preferences) {
    this.context = context;
    this.preferences = preferences;

    for (Mode m : Mode.values()) {
      score.put(m, 0);
      disqualified.put(m, false);
    }
  }

  public ContextDTO getContext() { return context; }
  public UserPreferencesDTO getPreferences() { return preferences; }

  public int getScore(Mode mode) { return score.get(mode); }
  public boolean isDisqualified(Mode mode) { return disqualified.get(mode); }

  public void addScore(Mode mode, int points, String reason, String ruleName) {
    if (Boolean.TRUE.equals(disqualified.get(mode))) return;
    score.put(mode, score.get(mode) + points);
    if (reason != null && !reason.isBlank()) reasons.add(reason);
    if (ruleName != null && !ruleName.isBlank()) rulesTriggered.add(ruleName);
  }

  public void disqualify(Mode mode, String reason, String ruleName) {
    disqualified.put(mode, true);
    score.put(mode, Integer.MIN_VALUE / 4);
    if (reason != null && !reason.isBlank()) reasons.add(reason);
    if (ruleName != null && !ruleName.isBlank()) rulesTriggered.add(ruleName);
  }

  public List<String> getReasons() { return reasons; }
  public List<String> getRulesTriggered() { return rulesTriggered; }

  public Mode getRecommended() { return recommended; }
  public void setRecommended(Mode recommended) { this.recommended = recommended; }

  public double getConfidenceScore() { return confidenceScore; }
  public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

  public EnumMap<Mode, Integer> getAllScores() { return score; }
}
