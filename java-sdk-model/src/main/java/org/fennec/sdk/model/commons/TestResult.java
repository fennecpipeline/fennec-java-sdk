package org.fennec.sdk.model.commons;

import lombok.*;

/**
 * Model representing an individual test result
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {

    /**
     * The test id
     */
    private String id;

    /**
     * The test duration in ms
     */
    private Long durationMs;

    /**
     * The test execution status
     */
    private TestStatus status;

    /**
     * The details (can be used for error or failure)
     */
    private String details;

}
