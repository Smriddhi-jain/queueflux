package io.queueflux.engine.runtime;

import io.queueflux.engine.random.DeterministicRandom;
import io.queueflux.model.scenario.ServiceDistribution;
import io.queueflux.model.scenario.ServiceTimeModel;

public final class ServiceTimeSampler {
    private ServiceTimeSampler() {}

    public static long sample(ServiceTimeModel model, long seed, String jobId) {
        if (model.distribution() == ServiceDistribution.CONSTANT) return model.constantMicros();
        DeterministicRandom random = DeterministicRandom.stream(seed, "service:" + jobId);
        double uniform = Math.max(Double.MIN_VALUE, 1d - random.nextDouble());
        return Math.max(1L, (long) Math.ceil(-Math.log(uniform) * model.meanMicros()));
    }
}
