package org.fennec.sdk.testing.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.fennec.sdk.model.commons.Deployment;
import org.fennec.sdk.model.commons.Link;
import org.fennec.sdk.model.commons.TestReport;
import org.fennec.sdk.model.events.*;
import org.slf4j.event.Level;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventTestsUtils {

    public static void testStartStageEvent(TimestampedEvent timestampedEvent, String stageName, String parallel,
            Deployment deployment) {
        assertThat(timestampedEvent, instanceOf(StartStageEvent.class));
        StartStageEvent event = (StartStageEvent) timestampedEvent;
        assertThat(event.getTimestamp(), notNullValue());
        assertThat(event.getStage(), equalTo(stageName));
        assertThat(event.getParallel(), equalTo(parallel));
        assertThat(event.getDeployment(), equalTo(deployment));
    }

    public static void testStageLogEvent(TimestampedEvent timestampedEvent, String stageName, Level level,
            String message) {
        assertThat(timestampedEvent, instanceOf(StageLogEvent.class));
        StageLogEvent event = (StageLogEvent) timestampedEvent;
        assertThat(event.getStage(), equalTo(stageName));
        assertThat(event.getTimestamp(), notNullValue());
        assertThat(event.getLevel(), equalTo(level));
        assertThat(event.getMessage(), containsString(message));
    }

    public static void testEndStageEvent(TimestampedEvent timestampedEvent, String stageName, String reason,
            TestReport report) {
        assertThat(timestampedEvent, instanceOf(EndStageEvent.class));
        EndStageEvent event = (EndStageEvent) timestampedEvent;
        assertThat(event.getTimestamp(), notNullValue());
        assertThat(event.getStage(), equalTo(stageName));
        assertThat(event.getReason(), anyOf(startsWith(reason), equalTo(reason)));
        assertThat(event.getTestReport(), equalTo(report));
    }

    public static void testUpdateJobEvent(TimestampedEvent timestampedEvent, String displayName) {
        assertThat(timestampedEvent, instanceOf(UpdateJobEvent.class));
        UpdateJobEvent event = (UpdateJobEvent) timestampedEvent;
        assertThat(event.getTimestamp(), notNullValue());
        assertThat(event.getDisplayName(), equalTo(displayName));
    }

    public static void testUpdateJobEvent(TimestampedEvent timestampedEvent, List<Link> links) {
        assertThat(timestampedEvent, instanceOf(UpdateJobEvent.class));
        UpdateJobEvent event = (UpdateJobEvent) timestampedEvent;
        assertThat(event.getTimestamp(), notNullValue());
        assertThat(event.getLinks(), equalTo(links));
    }

    public static <T extends TimestampedEvent> List<T> getEventsForType(List<TimestampedEvent> events, Class<T> type) {
        return events
                .stream()
                .filter(event -> type.isInstance(event))
                .map(event -> (T) event)
                .collect(Collectors.toList());
    }
}
