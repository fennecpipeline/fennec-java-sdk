package org.fennec.sdk.model.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * An event representing an update of the current Job<br/>
 * <i>It is not a Kubernetes Resource. The format is similar for usability</i>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "apiVersion", "kind", "timestamp", "displayName", })
@Accessors(prefix = { "_", "" })
@ToString
public class UpdateJobEvent implements TimestampedEvent {

    public static final String API_VERSION = "v1";

    public static final String UPDATE_JOB_EVENT_KIND = "UpdateJobEvent";

    private final String apiVersion = API_VERSION;

    private final String kind = UPDATE_JOB_EVENT_KIND;

    /**
     * The event timestamp
     */
    private Long timestamp;

    /**
     * The new Job display name
     */
    private String displayName;

}
