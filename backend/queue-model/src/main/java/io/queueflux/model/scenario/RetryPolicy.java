package io.queueflux.model.scenario;

/** Retry policy for transient processing failures. maxAttempts includes the first attempt. */
public record RetryPolicy(int maxAttempts, long initialBackoffMicros, long maxBackoffMicros,
                          double multiplier, double jitterFraction) {
    public RetryPolicy {
        if (maxAttempts < 1 || maxAttempts > 100) throw new IllegalArgumentException("maxAttempts must be between 1 and 100");
        if (initialBackoffMicros < 0 || maxBackoffMicros < initialBackoffMicros) {
            throw new IllegalArgumentException("retry backoff bounds are invalid");
        }
        if (!Double.isFinite(multiplier) || multiplier < 1) throw new IllegalArgumentException("retry multiplier must be finite and >= 1");
        if (!Double.isFinite(jitterFraction) || jitterFraction < 0 || jitterFraction > 1) {
            throw new IllegalArgumentException("jitterFraction must be between 0 and 1");
        }
    }

    public static RetryPolicy disabled() { return new RetryPolicy(1, 0, 0, 1, 0); }
}
