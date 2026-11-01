package io.queueflux.profiles.bullmq;

import io.queueflux.model.scenario.ScenarioDefinition;
import io.queueflux.model.scenario.ScenarioValidator;
import io.queueflux.profiles.api.SimulationProfile;

/**
 * Marks the supported QueueFlux generic subset intended for BullMQ comparisons.
 * This is not a BullMQ compatibility emulator and does not implement Redis locks,
 * stalled-job detection, worker heartbeats, or BullMQ's exact state transitions.
 */
public final class BullMqInspiredProfile implements SimulationProfile {
    @Override public String id() { return "bullmq-inspired"; }
    @Override public int version() { return 1; }

    @Override
    public void validate(ScenarioDefinition scenario) {
        ScenarioValidator.validate(scenario);
    }

    public String compatibilityNote() {
        return "QueueFlux token-bucket, retries, priorities and worker-capacity models are abstractions; validate against a real BullMQ deployment before drawing production conclusions.";
    }
}
