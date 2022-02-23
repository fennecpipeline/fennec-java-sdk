package org.fennec.sdk.pipeline.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fennec.sdk.model.commons.Deployment;

/**
 * A Simple Stage representation
 */
@Getter
@RequiredArgsConstructor
public class ExecStage implements Stage {

    private final String name;

    private final String parallel;

    private final Deployment deployment;

    private final SimpleStageHandler handler;

    public ExecStage(String name, SimpleStageHandler handler) {
        this.name = name;
        this.parallel = null;
        this.handler = handler;
        this.deployment = null;
    }

    public ExecStage(String name, String parallel, SimpleStageHandler handler) {
        this.name = name;
        this.parallel = parallel;
        this.handler = handler;
        this.deployment = null;
    }

    public ExecStage(String name, Deployment deployment, SimpleStageHandler handler) {
        this.name = name;
        this.parallel = null;
        this.handler = handler;
        this.deployment = deployment;
    }

}
