package org.fennec.sdk.model.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.event.Level;

/**
 * An event representing a log in a stage<br/>
 * <i>It is not a Kubernetes Resource. The format is similar for usability</i>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({ "apiVersion", "kind", "timestamp", "stage", "level", "message", })
@Accessors(prefix = { "_", "" })
@ToString
@Builder
public class StageLogEvent implements StageEvent {

    public static final String API_VERSION = "v1";

    public static final String STAGE_LOG_EVENT_KIND = "StageLogEvent";

    private final String apiVersion = API_VERSION;

    private final String kind = STAGE_LOG_EVENT_KIND;

    /**
     * The event timestamp
     */
    private Long timestamp;

    /**
     * The stage name
     */
    private String stage;

    /**
     * The log level
     */
    private Level level;

    /**
     * The message to log
     */
    private String message;

}
