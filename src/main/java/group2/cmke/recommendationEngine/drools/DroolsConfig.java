package group2.cmke.recommendationEngine.drools;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.io.ResourceType;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();

        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(ResourceFactory.newClassPathResource("rules/rules.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors:\n" + kb.getResults().toString());
        }

        return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }

    @Bean
    public KieSession kieSession(KieContainer kieContainer) {
        return kieContainer.newKieSession();
    }
}
