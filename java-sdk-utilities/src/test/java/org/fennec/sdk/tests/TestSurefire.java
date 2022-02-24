package org.fennec.sdk.tests;

import org.fennec.sdk.model.commons.TestReport;
import org.fennec.sdk.model.commons.TestResult;
import org.fennec.sdk.model.commons.TestStatus;
import org.fennec.sdk.model.commons.TestSuiteResult;
import org.fennec.sdk.utilities.tests.Surefire;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestSurefire {

    @Test
    void testSurefire() {
        TestReport report = new Surefire("src/test/resources/reports").getTestsResults("Unit tests");
        assertThat(report,
                equalTo(TestReport
                        .builder()
                        .type("Unit tests")
                        .suites(Arrays.asList(
                                TestSuiteResult
                                        .builder()
                                        .id("org.fennec.sdk.tests.TestSurefire")
                                        .durationMs(385L)
                                        .tests(Arrays.asList(TestResult
                                                .builder()
                                                .id("testSurefire")
                                                .durationMs(372L)
                                                .status(TestStatus.SUCCEEDED)
                                                .build()))
                                        .build(),
                                TestSuiteResult
                                        .builder()
                                        .id("org.fennec.rest.json.TestUtils")
                                        .durationMs(10L)
                                        .tests(Arrays.asList(TestResult
                                                        .builder()
                                                        .id("capitalizeLower")
                                                        .durationMs(8L)
                                                        .status(TestStatus.FAILED)
                                                        .details(
                                                                "org.opentest4j.AssertionFailedError: expected: <Foo> but was: <Foos>\n\tat org.fennec.rest.json.TestUtils.capitalizeLower(TestUtils.java:11)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeLower2")
                                                        .durationMs(8L)
                                                        .status(TestStatus.FAILED)
                                                        .details(
                                                                "org.opentest4j.AssertionFailedError: expected: <Bar> but was: <Bars>\n\tat org.fennec.rest.json.TestUtils.capitalizeLower2(TestUtils.java:21)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeLower3")
                                                        .durationMs(8L)
                                                        .status(TestStatus.FAILED)
                                                        .details(
                                                                "org.opentest4j.AssertionFailedError: expected: <John> but was: <Johns>\n\tat org.fennec.rest.json.TestUtils.capitalizeLower3(TestUtils.java:31)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeLower4")
                                                        .durationMs(1L)
                                                        .status(TestStatus.ERROR)
                                                        .details(
                                                                "java.lang.IllegalStateException: Noooo\n\tat org.fennec.rest.json.TestUtils.capitalizeLower4(TestUtils.java:42)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeLower5")
                                                        .durationMs(1L)
                                                        .status(TestStatus.ERROR)
                                                        .details(
                                                                "java.lang.IllegalStateException: Noooo\n\tat org.fennec.rest.json.TestUtils.capitalizeLower5(TestUtils.java:52)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeLower6")
                                                        .durationMs(1L)
                                                        .status(TestStatus.ERROR)
                                                        .details(
                                                                "java.lang.IllegalStateException: Noooo\n\tat org.fennec.rest.json.TestUtils.capitalizeLower6(TestUtils.java:62)\n")
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("capitalizeUpper")
                                                        .durationMs(0L)
                                                        .status(TestStatus.SKIPPED)
                                                        .build()))
                                        .build(),
                                TestSuiteResult
                                        .builder()
                                        .id("org.fennec.rest.json.HelloResourceTest")
                                        .durationMs(3320L)
                                        .tests(Arrays.asList(TestResult
                                                        .builder()
                                                        .id("testHelloJohn")
                                                        .durationMs(834L)
                                                        .status(TestStatus.SUCCEEDED)
                                                        .build(),
                                                TestResult
                                                        .builder()
                                                        .id("testHelloWorld")
                                                        .durationMs(8L)
                                                        .status(TestStatus.SUCCEEDED)
                                                        .build()))
                                        .build()

                                ))
                        .build()));

    }
}
