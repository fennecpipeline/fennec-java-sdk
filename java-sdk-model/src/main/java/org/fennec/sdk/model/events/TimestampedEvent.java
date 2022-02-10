package org.fennec.sdk.model.events;

/**
 * An event with a timestamp
 */
public interface TimestampedEvent {

    /**
     * @return the timestamp associated to this event
     */
    Long getTimestamp();

}
