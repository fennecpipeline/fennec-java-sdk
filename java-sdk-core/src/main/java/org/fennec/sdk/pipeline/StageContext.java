package org.fennec.sdk.pipeline;

import org.fennec.sdk.model.commons.TestReport;

public interface StageContext {

    String getStage();

    String getParallel();

    TestReport getTestResults();

    void setTestResults(TestReport testResults);

}
