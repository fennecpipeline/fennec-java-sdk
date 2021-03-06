package org.fennec.sdk.pipeline;

import lombok.Getter;
import lombok.Setter;
import org.fennec.sdk.model.commons.TestReport;

/**
 * The default implementation of {@link StageContext}
 */
@Getter
@Setter
public class StageContextDefaultImpl implements StageContext {

    private String version;

    private String stage;

    private String parallel;

    private TestReport TestResults;

    public StageContextDefaultImpl(String stage, String parallel, String version) {
        super();
        this.stage = stage;
        this.parallel = parallel;
        this.version = version;
    }
}
