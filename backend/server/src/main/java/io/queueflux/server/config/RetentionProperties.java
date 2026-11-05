package io.queueflux.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retention")
public record RetentionProperties(int runTtlDays) {
    public RetentionProperties {
        if (runTtlDays < 0) throw new IllegalArgumentException("run-ttl-days must be >= 0 (0 = disabled)");
    }
}
