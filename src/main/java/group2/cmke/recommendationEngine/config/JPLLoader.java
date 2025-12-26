package group2.cmke.recommendationEngine.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class JPLLoader {

  @PostConstruct
  public void init() {
    try {
      System.loadLibrary("jpl");
      System.out.println("JPL library has been successfully loaded! :)");
    } catch (UnsatisfiedLinkError e) {
      throw new RuntimeException("Failed to load JPL native library", e);
    }
  }
}
