package io.queueflux.engine.random;

import java.nio.charset.StandardCharsets;

/** Version-stable SplitMix64 stream with a stable, named seed derivation. */
public final class DeterministicRandom {
    private long state;

    private DeterministicRandom(long seed) {
        this.state = seed;
    }

    public static DeterministicRandom stream(long seed, String channel) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : channel.getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xffL;
            hash *= 0x100000001b3L;
        }
        return new DeterministicRandom(mix64(seed ^ hash));
    }

    /** Return a reproducible double in [0, 1), using the documented SplitMix64 step. */
    public double nextDouble() {
        long value = (state += 0x9e3779b97f4a7c15L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
