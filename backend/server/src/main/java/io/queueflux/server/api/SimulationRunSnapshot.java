package io.queueflux.server.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record SimulationRunSnapshot(
        UUID id,
        String status,
        String scenarioName,
        long seed,
        Instant submittedAt,
        Instant completedAt,
        long virtualTimeMicros,
        long processedEvents,
        JsonNode scenario,
        JsonNode result,
        String errorMessage) {}
