package org.fennec.sdk.model.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The type of deployment (Load, Rollback, Fallback)
 */
public enum DeploymentType {

    @JsonProperty("Load") LOAD,
    @JsonProperty("Rollback") ROLLBACK,
    @JsonProperty("Fallback") FALLBACK

}
