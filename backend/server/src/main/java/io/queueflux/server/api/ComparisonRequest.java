package io.queueflux.server.api;

import io.queueflux.model.scenario.ScenarioDefinition;

public record ComparisonRequest(ScenarioDefinition scenario, int replications, long seedStride) {
    public void validate() {
        if (scenario == null) throw new IllegalArgumentException("scenario is required");
        if (replications < 2 || replications > 30) throw new IllegalArgumentException("replications must be between 2 and 30");
        if (seedStride < 1) throw new IllegalArgumentException("seedStride must be >= 1");
    }
}
