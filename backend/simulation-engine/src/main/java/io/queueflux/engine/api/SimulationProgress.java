package io.queueflux.engine.api;

public record SimulationProgress(long virtualTimeMicros, long processedEvents, long scheduledEvents) {}
