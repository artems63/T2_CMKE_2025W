package group2.cmke.recommendationEngine.drools.facts;

import group2.cmke.recommendationEngine.dto.ContextDTO;
import group2.cmke.recommendationEngine.dto.UserPreferencesDTO;

// We use this fact inside our drools engine. This fact contains both DTOs from the user and controller.
public class Fact {

  public UserPreferencesDTO userPreferences;
  public ContextDTO context;

  public String recommendation;

  public Fact(UserPreferencesDTO userPreferences, ContextDTO context) {
    this.userPreferences = userPreferences;
    this.context = context;
  }
}
