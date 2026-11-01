package io.queueflux.engine.api;

public record TraceEvent(long timeMicros, String type, String jobId, String detail) {}
