package org.fennec.sdk.model.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The test status (Failed, Error, Succeeded, Skipped)
 */
public enum TestStatus {

    @JsonProperty("Failed") FAILED,
    @JsonProperty("Error") ERROR,
    @JsonProperty("Succeeded") SUCCEEDED,
    @JsonProperty("Skipped") SKIPPED

}
