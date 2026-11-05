package io.queueflux.server.config;

import io.queueflux.engine.DefaultSimulationEngine;
import io.queueflux.engine.api.SimulationEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RetentionProperties.class)
public class SimulationConfiguration {
    @Bean
    SimulationEngine simulationEngine() {
        return new DefaultSimulationEngine();
    }
}
