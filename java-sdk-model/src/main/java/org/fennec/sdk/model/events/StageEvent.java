package org.fennec.sdk.model.events;

/**
 * An event with a stage
 */
public interface StageEvent extends TimestampedEvent {

    /**
     * @return the stage associated to this event
     */
    String getStage();

}
