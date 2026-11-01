package io.queueflux.engine.api;

public interface SimulationEngine {
    SimulationResult run(SimulationRequest request, SimulationObserver observer);

    default SimulationResult run(SimulationRequest request) {
        return run(request, SimulationObserver.noop());
    }
}
