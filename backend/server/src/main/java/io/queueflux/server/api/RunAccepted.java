package io.queueflux.server.api;

import java.util.UUID;

public record RunAccepted(UUID id, String status, String statusUrl, String eventsUrl) {}
