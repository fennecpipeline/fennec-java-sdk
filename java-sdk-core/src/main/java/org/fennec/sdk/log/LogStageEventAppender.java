package org.fennec.sdk.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.fennec.sdk.pipeline.PipelineConstants;
import org.fennec.sdk.pipeline.StageEventPublisher;
import org.slf4j.event.Level;

/**
 * The log event appender is sending logs to the stage event publisher
 */
public class LogStageEventAppender extends AppenderBase<ILoggingEvent> {

    private static final StageEventPublisher STAGE_EVENT_PUBLISHER = new StageEventPublisher();

    @Override
    protected void append(ILoggingEvent event) {
        STAGE_EVENT_PUBLISHER.log(event.getMDCPropertyMap().get(PipelineConstants.STAGE_NAME),
                Level.valueOf(event.getLevel().toString()),
                LogUtils.getFormattedMessage(event));
    }
}