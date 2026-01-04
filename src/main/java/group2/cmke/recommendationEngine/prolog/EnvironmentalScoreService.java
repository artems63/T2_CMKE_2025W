package group2.cmke.recommendationEngine.prolog;

import org.jpl7.Query;
import org.jpl7.Term;
import org.jpl7.Atom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

@Service
public class EnvironmentalScoreService {

    private static final String LOG_PATH = System.getenv("DEBUG_LOG_PATH") != null ? System.getenv("DEBUG_LOG_PATH") : (System.getProperty("user.dir") + java.io.File.separator + ".cursor" + java.io.File.separator + "debug.log");

    private volatile boolean consulted = false;

    private void ensureConsulted() {
        if (consulted) return;

        synchronized (this) {
            if (consulted) return;

            ClassPathResource resource = new ClassPathResource("prolog/environmental_score.pl");
            if (!resource.exists()) {
                throw new IllegalStateException("Missing resource: prolog/environmental_score.pl");
            }

            // Test if ANY Query works at all (diagnostic)
            try {
                Query testQuery = new Query("true");
                boolean testResult = testQuery.hasSolution();
                testQuery.close();
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Test query result\",\"data\":{\"testQueryWorks\":%s},\"timestamp\":%d}\n", testResult, System.currentTimeMillis()));
                    fw.close();
                } catch (Exception ex) {}
                // #endregion
            } catch (Exception testEx) {
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Test query failed\",\"data\":{\"exception\":\"%s\"},\"timestamp\":%d}\n", testEx.getClass().getName() + ": " + testEx.getMessage(), System.currentTimeMillis()));
                    fw.close();
                } catch (Exception ex) {}
                // #endregion
                throw new RuntimeException("JPL is not working - even simple queries fail: " + testEx.getMessage(), testEx);
            }

            // Use standard consult() method to load Prolog file
            try {
                // Get the absolute path to the Prolog file
                java.io.File prologFile = resource.getFile();
                String filePath = prologFile.getAbsolutePath();
                
                // Use consult/1 to load the Prolog file using Term API
                Query consultQuery = new Query("consult", new Term[] {new Atom(filePath.replace("\\", "/"))});
                boolean success = consultQuery.hasSolution();
                consultQuery.close();
                
                if (!success) {
                    throw new IllegalStateException("Failed to consult Prolog file: " + filePath);
                }
                
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Prolog file consulted successfully\",\"data\":{\"filePath\":\"%s\"},\"timestamp\":%d}\n", filePath.replace("\\", "/"), System.currentTimeMillis()));
                    fw.close();
                } catch (Exception ex) {}
                // #endregion
                
                consulted = true;
            } catch (java.io.IOException e) {
                // If getFile() fails (e.g., in JAR), try alternative approach using stream
                try (InputStream in = resource.getInputStream()) {
                    // Create temporary file
                    java.io.File tempFile = java.io.File.createTempFile("environmental_score", ".pl");
                    tempFile.deleteOnExit();
                    
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                        in.transferTo(out);
                    }
                    
                    String filePath = tempFile.getAbsolutePath();
                    Query consultQuery = new Query("consult", new Term[] {new Atom(filePath.replace("\\", "/"))});
                    boolean success = consultQuery.hasSolution();
                    consultQuery.close();
                    
                    if (!success) {
                        throw new IllegalStateException("Failed to consult Prolog file from stream: " + filePath);
                    }
                    
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Prolog file consulted from stream\",\"data\":{\"filePath\":\"%s\"},\"timestamp\":%d}\n", filePath.replace("\\", "/"), System.currentTimeMillis()));
                        fw.close();
                    } catch (Exception ex) {}
                    // #endregion
                    
                    consulted = true;
                } catch (Exception ex) {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                        fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Consult failed\",\"data\":{\"exception\":\"%s\"},\"timestamp\":%d}\n", ex.getClass().getName() + ": " + ex.getMessage(), System.currentTimeMillis()));
                        fw.close();
                    } catch (Exception ex2) {}
                    // #endregion
                    throw new RuntimeException("Failed to load environmental_score.pl via consult: " + ex.getMessage(), ex);
                }
            } catch (Exception e) {
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.ensureConsulted\",\"message\":\"Consult failed\",\"data\":{\"exception\":\"%s\"},\"timestamp\":%d}\n", e.getClass().getName() + ": " + e.getMessage(), System.currentTimeMillis()));
                    fw.close();
                } catch (Exception ex) {}
                // #endregion
                throw new RuntimeException("Failed to load environmental_score.pl via consult: " + e.getMessage(), e);
            }
        }
    }

    public double environmentalFactor(double distanceMeters, boolean weatherOk, boolean wantsSustainable) {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
            fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:66\",\"message\":\"Method entry\",\"data\":{\"distanceMeters\":%f,\"weatherOk\":%s,\"wantsSustainable\":%s},\"timestamp\":%d}\n", distanceMeters, weatherOk, wantsSustainable, System.currentTimeMillis()));
            fw.close();
        } catch (Exception e) {}
        // #endregion
        ensureConsulted();

        String weatherAtom = weatherOk ? "true" : "false";
        String sustainableAtom = wantsSustainable ? "true" : "false";

        String goal = String.format(
                "environmental_factor(%d, %s, %s, F).",
                (long) distanceMeters, weatherAtom, sustainableAtom
        );

        Map<String, Term> sol = null;
        double result = 0.0;
        Query q = null;
        try {
            q = new Query(goal);
            // #region agent log
            boolean queryHasSolution = false;
            try {
                queryHasSolution = q.hasSolution();
                java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:64\",\"message\":\"Query hasSolution check\",\"data\":{\"hasSolution\":%s,\"goal\":\"%s\"},\"timestamp\":%d}\n", queryHasSolution, goal.replace("\"", "\\\""), System.currentTimeMillis()));
                fw.close();
            } catch (Exception e) {}
            // #endregion
            sol = q.oneSolution();
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:65\",\"message\":\"Query oneSolution result\",\"data\":{\"solIsNull\":%s},\"timestamp\":%d}\n", (sol == null), System.currentTimeMillis()));
                fw.close();
            } catch (Exception e) {}
            // #endregion
            if (sol != null && sol.get("F") != null) {
                result = sol.get("F").doubleValue();
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:135\",\"message\":\"Extracted result from Prolog\",\"data\":{\"result\":%f},\"timestamp\":%d}\n", result, System.currentTimeMillis()));
                    fw.close();
                } catch (Exception e) {}
                // #endregion
            } else {
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                    fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:135\",\"message\":\"Result extraction failed - Prolog query returned no solution\",\"data\":{\"solIsNull\":%s,\"fIsNull\":%s},\"timestamp\":%d}\n", (sol == null), (sol != null && sol.get("F") == null), System.currentTimeMillis()));
                    fw.close();
                } catch (Exception e) {}
                // #endregion
                throw new IllegalStateException("Prolog query returned no solution for environmental_factor(" + (long)distanceMeters + ", " + weatherAtom + ", " + sustainableAtom + ", F)");
            }
        } catch (Exception e) {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
                fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:65\",\"message\":\"Query exception - Prolog failed\",\"data\":{\"exception\":\"%s\"},\"timestamp\":%d}\n", e.getClass().getName() + ": " + e.getMessage(), System.currentTimeMillis()));
                fw.close();
            } catch (Exception ex) {}
            // #endregion
            // throw exception if Prolog fails
            throw new RuntimeException("Prolog query failed for environmental_factor: " + e.getMessage(), e);
        } finally {
            if (q != null) {
                q.close();
            }
        }
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(LOG_PATH, true);
            fw.write(String.format("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"EnvironmentalScoreService.environmentalFactor:66\",\"message\":\"Method exit\",\"data\":{\"result\":%f,\"goal\":\"%s\"},\"timestamp\":%d}\n", result, goal.replace("\"", "\\\""), System.currentTimeMillis()));
            fw.close();
        } catch (Exception e) {}
        // #endregion
        return result;
    }

    public double modeEnvironmentalFactor(String modeName, double distanceMeters, boolean weatherOk) {
        ensureConsulted();
        
        // Map Java enum names to Prolog atom names
        String prologMode;
        if (modeName.equals("E_BIKE")) {
            prologMode = "e_bike";
        } else if (modeName.equals("GAS_CAR")) {
            prologMode = "gas_car";
        } else if (modeName.equals("ELECTRIC_CAR")) {
            prologMode = "electric_car";
        } else if (modeName.equals("BIKESHARE")) {
            prologMode = "bikeshare";
        } else if (modeName.equals("PUBLIC_TRANSPORT")) {
            prologMode = "public_transport";
        } else {
            prologMode = modeName.toLowerCase();
        }
        
        String weatherAtom = weatherOk ? "true" : "false";
        String goal = String.format(
                "mode_environmental_factor(%s, %d, %s, F).",
                prologMode, (long) distanceMeters, weatherAtom
        );
        
        Map<String, Term> sol = null;
        double result = 0.0;
        Query q = null;
        try {
            q = new Query(goal);
            sol = q.oneSolution();
            if (sol != null && sol.get("F") != null) {
                result = sol.get("F").doubleValue();
            } else {
                // No fallback - throw exception if no solution
                throw new IllegalStateException("Prolog query returned no solution for mode_environmental_factor(" + prologMode + ", " + (long)distanceMeters + ", " + weatherAtom + ", F)");
            }
        } catch (Exception e) {
            // No fallback - throw exception if Prolog fails
            throw new RuntimeException("Prolog query failed for mode_environmental_factor: " + e.getMessage(), e);
        } finally {
            if (q != null) {
                q.close();
            }
        }
        return result;
    }
}
