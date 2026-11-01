package io.queueflux.profiles.api;

import io.queueflux.model.scenario.ScenarioDefinition;

public interface SimulationProfile {
    String id();
    int version();
    void validate(ScenarioDefinition scenario);
}
