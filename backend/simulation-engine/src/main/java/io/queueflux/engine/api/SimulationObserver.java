package io.queueflux.engine.api;

@FunctionalInterface
public interface SimulationObserver {
    void onProgress(SimulationProgress progress);

    static SimulationObserver noop() {
        return ignored -> {};
    }
}
