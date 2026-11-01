package io.queueflux.model.scenario;

/** Global token-bucket limiter for job starts on one queue. A zero rate disables limiting. */
public record RateLimitPolicy(double ratePerSecond, int burstCapacity) {
    public RateLimitPolicy {
        if (!Double.isFinite(ratePerSecond) || ratePerSecond < 0) {
            throw new IllegalArgumentException("ratePerSecond must be finite and >= 0");
        }
        if (burstCapacity < 1) throw new IllegalArgumentException("burstCapacity must be >= 1");
    }

    public static RateLimitPolicy unlimited() { return new RateLimitPolicy(0, Integer.MAX_VALUE); }
}
