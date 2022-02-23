package org.fennec.sdk.model.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Accessors;
import org.fennec.sdk.model.commons.TestReport;

/**
 * An event representing the end of a stage<br/>
 * <i>It is not a Kubernetes Resource. The format is similar for usability</i>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({ "apiVersion", "kind", "timestamp", "stage", "reason", "testResults", })
@Accessors(prefix = { "_", "" })
@ToString
@Builder
public class EndStageEvent implements StageEvent {

    public static final String API_VERSION = "v1";

    public static final String END_STAGE_EVENT_KIND = "EndStageEvent";

    private final String apiVersion = API_VERSION;

    private final String kind = END_STAGE_EVENT_KIND;

    /**
     * The event timestamp
     */
    private Long timestamp;

    /**
     * The stage name
     */
    private String stage;

    /**
     * The reason for failure (if applicable)
     */
    private String reason;

    /**
     * The test report (if applicable)
     */
    private TestReport testReport;

}
