package org.fennec.sdk.model.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Accessors;
import org.fennec.sdk.model.commons.Deployment;

/**
 * An event representing the beginning of a stage<br/>
 * <i>It is not a Kubernetes Resource. The format is similar for usability</i>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "apiVersion", "kind", "timestamp", "stage", "parallel", "deployment", })
@Accessors(prefix = { "_", "" })
@ToString
public class StartStageEvent implements StageEvent {

    public static final String API_VERSION = "v1";

    public static final String START_STAGE_EVENT_KIND = "StartStageEvent";

    private final String apiVersion = API_VERSION;

    private final String kind = START_STAGE_EVENT_KIND;

    /**
     * The event timestamp
     */
    private Long timestamp;

    /**
     * The stage name
     */
    private String stage;

    /**
     * Group stage for parallel execution (if applicable)
     */
    private String parallel;

    /**
     * Add deployment information for this stage (if applicable)
     */
    private Deployment deployment;

}
