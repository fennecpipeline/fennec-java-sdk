package org.fennec.sdk.model.commons;

import lombok.*;

import java.util.List;

/**
 * Model representing a test report
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestReport {

    /**
     * The test report type (free text: example: Unit tests, integration tests, pen tests...)
     */
    private String type;

    /**
     * The tests suites results
     */
    private List<TestSuiteResult> suites;

}
