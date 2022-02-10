package org.fennec.sdk.model.commons;

import lombok.*;

import java.util.List;

/**
 * Model representing an individual test suite result
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSuiteResult {

    /**
     * The suite id
     */
    private String id;

    /**
     * The suite duration in ms
     */
    private Long durationMs;

    /**
     * The list of test result
     */
    private List<TestResult> tests;

}
