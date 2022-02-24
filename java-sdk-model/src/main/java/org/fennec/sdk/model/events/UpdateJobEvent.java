package org.fennec.sdk.model.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.Accessors;
import org.fennec.sdk.model.commons.Link;

import java.util.List;

/**
 * An event representing an update of the current Job<br>
 * <i>It is not a Kubernetes Resource. The format is similar for usability</i>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({ "apiVersion", "kind", "timestamp", "displayName", })
@Accessors(prefix = { "_", "" })
@ToString
@Builder
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

    /**
     * Some links to add to the jobs
     * It uses append.
     * If called a first time with two links and another one with three others. The final job will contain 5 links
     */
    private List<Link> links;

}
