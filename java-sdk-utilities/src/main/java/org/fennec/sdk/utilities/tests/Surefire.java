package org.fennec.sdk.utilities.tests;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.fennec.sdk.model.commons.TestReport;
import org.fennec.sdk.model.commons.TestResult;
import org.fennec.sdk.model.commons.TestStatus;
import org.fennec.sdk.model.commons.TestSuiteResult;
import org.fennec.sdk.utilities.data.XmlUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Surefire {

    /**
     * The report path is in <project folder>/target/surefire-reports
     */
    private static final String SUREFIRE_REPORT_PATH = "../target/surefire-reports";

    private final String reportPath;

    public static final Surefire surefire() {
        return new Surefire(SUREFIRE_REPORT_PATH);
    }

    public TestReport getTestsResults(String type) {
        TestReport testResults = new TestReport();
        testResults.setType(type);
        testResults.setSuites(Arrays
                .asList(new File(reportPath).listFiles((dir, name) -> name.endsWith(".xml")))
                .stream()
                .map(this::toTestSuiteResult)
                .collect(Collectors.toList()));
        return testResults;
    }

    private TestSuiteResult toTestSuiteResult(File file) {
        JsonNode node = XmlUtils.readXML(file);
        TestSuiteResult testSuiteResult = new TestSuiteResult();
        testSuiteResult.setId(node.get("name").asText());
        testSuiteResult.setDurationMs((long)(Float.valueOf(node.get("time").asText()) * 1000));
        List<TestResult> testResults = new ArrayList<>();
        node.get("testcase").forEach(testcase -> {
            testResults.add(toTestResult(testcase));
        });
        testSuiteResult.setTests(testResults);
        return testSuiteResult;
    }

    private TestResult toTestResult(JsonNode node) {
        TestResult testResult = new TestResult();
        testResult.setId(node.get("name").asText());
        testResult.setDurationMs((long)(Float.valueOf(node.get("time").asText()) * 1000));

        if (node.has("failure")) {
            testResult.setStatus(TestStatus.FAILED);
            testResult.setDetails(getDetails(node, "failure"));
        } else if (node.has("rerunFailure")) {
            testResult.setStatus(TestStatus.FAILED);
            testResult.setDetails(getDetails(node, "rerunFailure"));
        } else if (node.has("flakyFailure")) {
            testResult.setStatus(TestStatus.FAILED);
            testResult.setDetails(getDetails(node, "flakyFailure"));
        } else if (node.has("error")) {
            testResult.setStatus(TestStatus.ERROR);
            testResult.setDetails(getDetails(node, "error"));
        } else if (node.has("rerunError")) {
            testResult.setStatus(TestStatus.ERROR);
            testResult.setDetails(getDetails(node, "rerunError"));
        } else if (node.has("flakyError")) {
            testResult.setStatus(TestStatus.ERROR);
            testResult.setDetails(getDetails(node, "flakyError"));
        } else if (node.has("skipped")) {
            testResult.setStatus(TestStatus.SKIPPED);
        } else {
            testResult.setStatus(TestStatus.SUCCEEDED);
        }

        return testResult;
    }

    private String getDetails(JsonNode node, String nodeName) {
        if (node.get(nodeName).has("")) {
            return node.get(nodeName).get("").asText();
        } else if (node.get(nodeName).has("type")) {
            return node.get(nodeName).get("type").asText();
        }
        return null;
    }

}
