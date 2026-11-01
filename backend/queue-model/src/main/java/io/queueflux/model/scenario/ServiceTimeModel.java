package io.queueflux.model.scenario;

/** Service times are expressed in microseconds. */
public record ServiceTimeModel(ServiceDistribution distribution, long constantMicros, double meanMicros) {
    public ServiceTimeModel {
        if (distribution == null) throw new IllegalArgumentException("service distribution is required");
        if (distribution == ServiceDistribution.CONSTANT && constantMicros <= 0) {
            throw new IllegalArgumentException("constantMicros must be > 0");
        }
        if (distribution == ServiceDistribution.EXPONENTIAL && (!Double.isFinite(meanMicros) || meanMicros <= 0)) {
            throw new IllegalArgumentException("meanMicros must be finite and > 0");
        }
    }

    public static ServiceTimeModel constant(long micros) {
        return new ServiceTimeModel(ServiceDistribution.CONSTANT, micros, micros);
    }

    public static ServiceTimeModel exponential(double meanMicros) {
        return new ServiceTimeModel(ServiceDistribution.EXPONENTIAL, 0, meanMicros);
    }
}
