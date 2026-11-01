package io.queueflux.engine.scheduler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EventSchedulerTest {
    @Test
    void ordersEqualTimestampEventsByPhaseAndThenInsertionSequence() {
        EventScheduler scheduler = new EventScheduler();
        scheduler.schedule(100, EventType.JOB_ARRIVAL, "arrive-1", "q");
        scheduler.schedule(100, EventType.JOB_COMPLETED, "complete-1", "q");
        scheduler.schedule(100, EventType.JOB_ARRIVAL, "arrive-2", "q");

        assertThat(scheduler.poll().type()).isEqualTo(EventType.JOB_COMPLETED);
        assertThat(scheduler.poll().jobId()).isEqualTo("arrive-1");
        assertThat(scheduler.poll().jobId()).isEqualTo("arrive-2");
        assertThat(scheduler.isEmpty()).isTrue();
    }
}
