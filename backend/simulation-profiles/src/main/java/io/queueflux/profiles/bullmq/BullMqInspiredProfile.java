package io.queueflux.profiles.bullmq;

import io.queueflux.model.scenario.ScenarioDefinition;
import io.queueflux.model.scenario.ScenarioValidator;
import io.queueflux.profiles.api.SimulationProfile;

/** Names the first compatibility direction; this is not a claim of exact BullMQ runtime equivalence. */
public final class BullMqInspiredProfile implements SimulationProfile {
    @Override public String id() { return "bullmq-inspired"; }
    @Override public int version() { return 1; }
    @Override public void validate(ScenarioDefinition scenario) { ScenarioValidator.validate(scenario); }
}
