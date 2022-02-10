package org.fennec.sdk.model.commons;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a deployment stage
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    /**
     * The deployment target (free text: example staging)
     */
    private String target;

    /**
     * A list of tag to identify the deployment (example: region=eu-west-1, customer=customer-1)
     */
    private Map<String, String> tags;

    /**
     * The deployment type
     */
    private DeploymentType type;

    /**
     * Instantiate a deployment
     *
     * @param target: the deployment target
     * @param type: the deployment type
     */
    public Deployment(String target, DeploymentType type) {
        super();
        this.target = target;
        tags = new HashMap<>();
        this.type = type;
    }

    /**
     * Instantiate a deployment with a target and an indicator
     *
     * @param target: the deployment target
     * @param indicator: an indicator (ie: region)
     * @param valueIndicator: the value for this indicator (ie: eu-west-1)
     * @param type: the deployment type
     */
    public Deployment(String target, String indicator, String valueIndicator, DeploymentType type) {
        super();
        this.target = target;
        tags = new HashMap<>();
        tags.put(indicator, valueIndicator);
        this.type = type;
    }

}
