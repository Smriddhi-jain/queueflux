package io.queueflux.engine.clock;

public final class VirtualClock {
    private long nowMicros;

    public long nowMicros() { return nowMicros; }

    public void advanceTo(long nextTimeMicros) {
        if (nextTimeMicros < nowMicros) throw new IllegalArgumentException("virtual clock cannot move backwards");
        nowMicros = nextTimeMicros;
    }
}
