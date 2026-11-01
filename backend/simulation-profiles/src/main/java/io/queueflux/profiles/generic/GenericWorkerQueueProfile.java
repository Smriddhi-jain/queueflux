package io.queueflux.profiles.generic;

import io.queueflux.model.scenario.ScenarioDefinition;
import io.queueflux.model.scenario.ScenarioValidator;
import io.queueflux.profiles.api.SimulationProfile;

public final class GenericWorkerQueueProfile implements SimulationProfile {
    @Override public String id() { return "generic-worker-queue"; }
    @Override public int version() { return 1; }
    @Override public void validate(ScenarioDefinition scenario) { ScenarioValidator.validate(scenario); }
}
