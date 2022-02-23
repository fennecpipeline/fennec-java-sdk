package org.fennec.sdk.testing.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fennec.sdk.model.events.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The log event appender is sending logs to the stage event publisher
 */
@Getter
public class TestingEventAppender extends AppenderBase<ILoggingEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private List<String> rawEvents = new ArrayList<>();
    private List<TimestampedEvent> events = new ArrayList<>();
    private List<String> unmatched = new ArrayList<>();
    private List<EventsInError> inError = new ArrayList<>();

    /**
     * @param line the line to analyze
     * @return true if this looks like an {@link StartStageEvent}
     */
    private static boolean isMatchingStartStageEvent(String line) {
        return isMatchingApiVersionAndKind(StartStageEvent.API_VERSION, StartStageEvent.START_STAGE_EVENT_KIND, line);
    }

    /**
     * @param line the line to analyze
     * @return true if this looks like an {@link EndStageEvent}
     */
    private static boolean isMatchingEndStageEvent(String line) {
        return isMatchingApiVersionAndKind(EndStageEvent.API_VERSION, EndStageEvent.END_STAGE_EVENT_KIND, line);
    }

    /**
     * @param line the line to analyze
     * @return true if this looks like an {@link StageLogEvent}
     */
    private static boolean isMatchingStageLogEvent(String line) {
        return isMatchingApiVersionAndKind(StageLogEvent.API_VERSION, StageLogEvent.STAGE_LOG_EVENT_KIND, line);
    }

    /**
     * @param line the line to analyze
     * @return true if this looks like an {@link UpdateJobEvent}
     */
    private static boolean isMatchingUpdateJobEvent(String line) {
        return isMatchingApiVersionAndKind(UpdateJobEvent.API_VERSION, UpdateJobEvent.UPDATE_JOB_EVENT_KIND, line);
    }

    /**
     * @param expectedApiVersion
     * @param expectedKind
     * @param line
     * @return true if apiVersion and kind matches
     */
    private static boolean isMatchingApiVersionAndKind(String expectedApiVersion, String expectedKind, String line) {
        return line != null && line.contains(String.format("{\"apiVersion\":\"%s\"",
                expectedApiVersion)) && line.contains(String.format("\"kind\":\"%s\"", expectedKind));
    }

    /**
     * Reinitialize the events
     */
    public void clear() {
        rawEvents.clear();
        events.clear();
        unmatched.clear();
        inError.clear();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String eventPayload = event.getFormattedMessage();
        rawEvents.add(eventPayload);
        try {
            if (isMatchingStartStageEvent(eventPayload)) {
                events.add(OBJECT_MAPPER.readValue(eventPayload, StartStageEvent.class));
            } else if (isMatchingStageLogEvent(eventPayload)) {
                events.add(OBJECT_MAPPER.readValue(eventPayload, StageLogEvent.class));
            } else if (isMatchingEndStageEvent(eventPayload)) {
                events.add(OBJECT_MAPPER.readValue(eventPayload, EndStageEvent.class));
            } else if (isMatchingUpdateJobEvent(eventPayload)) {
                events.add(OBJECT_MAPPER.readValue(eventPayload, UpdateJobEvent.class));
            } else {
                unmatched.add(eventPayload);
            }
        } catch (Exception e) {
            inError.add(new EventsInError(eventPayload, e));
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static class EventsInError {
        private final String event;
        private final Exception error;
    }
}