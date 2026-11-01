package io.queueflux.server.config;

import io.queueflux.engine.DefaultSimulationEngine;
import io.queueflux.engine.api.SimulationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimulationConfiguration {
    @Bean
    SimulationEngine simulationEngine() {
        return new DefaultSimulationEngine();
    }
}
