package io.queueflux.model.scenario;

/** Arrival rate is measured in jobs per second. */
public record ArrivalModel(ArrivalMode mode, double ratePerSecond) {
    public ArrivalModel {
        if (mode == null) throw new IllegalArgumentException("arrival mode is required");
        if (!Double.isFinite(ratePerSecond) || ratePerSecond <= 0) {
            throw new IllegalArgumentException("ratePerSecond must be finite and > 0");
        }
    }

    public static ArrivalModel constantRate(double jobsPerSecond) {
        return new ArrivalModel(ArrivalMode.CONSTANT, jobsPerSecond);
    }

    public static ArrivalModel poisson(double jobsPerSecond) {
        return new ArrivalModel(ArrivalMode.POISSON, jobsPerSecond);
    }
}
