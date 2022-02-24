package org.fennec.sdk.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.fennec.sdk.model.commons.Deployment;
import org.fennec.sdk.model.commons.Link;
import org.fennec.sdk.model.commons.TestReport;
import org.fennec.sdk.model.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * The stage event publisher is providing capability to send stages information (start, end, log) to the side car
 * container
 */
public class StageEventPublisher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("fennec-print-events");

    /**
     * Format the exception to a string
     *
     * @param t the throwable to format
     * @return the exception to display
     */
    private static String formatException(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Update job
     *
     * @param newName the new name
     * @param links the links
     */
    public void updateJob(String newName, List<Link> links) {
        printStageEvent(new UpdateJobEvent(System.currentTimeMillis(), newName, links));
    }

    /**
     * Start a stage
     *
     * @param stageName    the stage name
     * @param parallelName the parallel
     * @param deployment   the deployment information
     */
    public void start(String stageName, String parallelName, Deployment deployment) {
        printStageEvent(new StartStageEvent(System.currentTimeMillis(), stageName, parallelName, deployment));
    }

    /**
     * Finish a stage successfully
     *
     * @param stageName  the stage name
     * @param testReport the test report
     */
    public void end(String stageName, TestReport testReport) {
        printStageEvent(new EndStageEvent(System.currentTimeMillis(), stageName, null, testReport));
    }

    /**
     * Finish a stage with a throwable
     *
     * @param stageName  the stage name
     * @param t          the error
     * @param testReport the test report
     */
    public void error(String stageName, Throwable t, TestReport testReport) {
        printStageEvent(new EndStageEvent(System.currentTimeMillis(), stageName, formatException(t), testReport));
    }

    /**
     * Finish a stage with error
     *
     * @param stageName the stage name
     * @param error     the error as string
     */
    public void error(String stageName, String error) {
        printStageEvent(new EndStageEvent(System.currentTimeMillis(), stageName, error, null));
    }

    /**
     * Log to a stage
     *
     * @param stageName the stage name
     * @param level     the log level
     * @param message   the message
     */
    public void log(String stageName, Level level, String message) {
        printStageEvent(new StageLogEvent(System.currentTimeMillis(), stageName, level, message));
    }

    @SneakyThrows
    private <T extends TimestampedEvent> void printStageEvent(T data) {
        if (EVENT_LOGGER.isInfoEnabled()) {
            EVENT_LOGGER.info(OBJECT_MAPPER.writeValueAsString(data));
        }
    }
}
