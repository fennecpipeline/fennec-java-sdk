package org.fennec.sdk.exec.local;

import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import org.fennec.sdk.exec.common.CommandOutput;
import org.fennec.sdk.exec.common.ExecCommandException;
import org.fennec.sdk.model.events.TimestampedEvent;
import org.fennec.sdk.pipeline.Pipeline;
import org.fennec.sdk.testing.utils.TestingEventAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.fennec.sdk.error.Fail.fail;
import static org.fennec.sdk.pipeline.Pipeline.stage;
import static org.fennec.sdk.testing.utils.EventTestsUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TestLocalExecService {

    TestingEventAppender testingEventAppender = (TestingEventAppender) ((Logger) LoggerFactory.getLogger(
            "fennec-print-events")).getAppender("STDOUT");

    @BeforeEach
    void init() {
        testingEventAppender.clear();
        Pipeline.configure(() -> {
            Assertions.fail("Test failed. Current event list: " + testingEventAppender
                    .getRawEvents()
                    .stream()
                    .collect(Collectors.joining("\n")));
        });
    }

    @AfterEach
    void tearDown() {
        testingEventAppender.clear();
    }

    @SneakyThrows
    void runSimpleTestWithLevel(Level level) {

        CompletableFuture<CommandOutput> completableFuture = new CompletableFuture<>();

        stage("Init", context -> {
            try {
                LocalExecService localExecService = new LocalExecService(System.getProperty("user.dir"), level);
                CommandOutput output = localExecService.exec("echo", "Hello\nworld");
                completableFuture.complete(output);
            } catch (ExecCommandException e) {
                fail("Command must be success", e);
            }
        });

        CommandOutput output = completableFuture.get(200, TimeUnit.MILLISECONDS);
        assertThat(output.getStatus(), equalTo(0));
        assertThat(output.getData(), equalTo("Hello\nworld"));

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(events, hasSize(4));
        testStartStageEvent(events.get(0), "Init", null, null);
        testStageLogEvent(events.get(1), "Init", level, "Hello");
        testStageLogEvent(events.get(2), "Init", level, "world");
        testEndStageEvent(events.get(3), "Init", null, null);
    }

    @Test
    @SneakyThrows
    void testSimpleCommand() {
        runSimpleTestWithLevel(Level.TRACE);
        tearDown();
        runSimpleTestWithLevel(Level.DEBUG);
        tearDown();
        runSimpleTestWithLevel(Level.INFO);
        tearDown();
        runSimpleTestWithLevel(Level.WARN);
        tearDown();
        runSimpleTestWithLevel(Level.ERROR);
    }

    @Test
    @SneakyThrows
    void testCommandDoesNotExist() {

        CompletableFuture<ExecCommandException> commandMustHaveFail = new CompletableFuture<>();
        CompletableFuture<Void> pipelineMustHaveFail = new CompletableFuture<>();

        Pipeline.configure(() -> {
            pipelineMustHaveFail.complete(null);
        });

        stage("Init", context -> {
            try {
                LocalExecService localExecService = new LocalExecService(System.getProperty("user.dir"), Level.INFO);
                CommandOutput output = localExecService.exec("fovhofv", "hioihve");
            } catch (ExecCommandException e) {
                commandMustHaveFail.complete(e);
                fail("Error during init: " + e.getMessage(), e);
            }
        });

        ExecCommandException output = commandMustHaveFail.get(100L, TimeUnit.MILLISECONDS);
        assertThat(output.getMessage(), equalTo("Exec Command [fovhofv, hioihve] failed with error"));

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());
        List<TimestampedEvent> events = testingEventAppender.getEvents();

        pipelineMustHaveFail.get(100, TimeUnit.MILLISECONDS);

        assertThat(events, hasSize(2));
        testStartStageEvent(events.get(0), "Init", null, null);
        testEndStageEvent(events.get(1),
                "Init",
                "org.fennec.sdk.error.CancelJobException: Error during init: Exec Command [fovhofv, hioihve] failed with error",
                null);
    }

    @Test
    @SneakyThrows
    void testNon0StatusCode() {

        CompletableFuture<ExecCommandException> commandMustHaveFail = new CompletableFuture<>();
        CompletableFuture<Void> pipelineMustHaveFail = new CompletableFuture<>();

        Pipeline.configure(() -> {
            pipelineMustHaveFail.complete(null);
        });

        stage("Init", context -> {
            try {
                LocalExecService localExecService = new LocalExecService(System.getProperty("user.dir"), Level.INFO);
                CommandOutput output = localExecService.exec("/bin/sh", "src/test/resources/failing-script.sh");
            } catch (ExecCommandException e) {
                commandMustHaveFail.complete(e);
                fail("Error during init: " + e.getMessage(), e);
            }
        });

        ExecCommandException output = commandMustHaveFail.get(100L, TimeUnit.MILLISECONDS);
        assertThat(output.getMessage(),
                equalTo("Exec Command [/bin/sh, src/test/resources/failing-script.sh] failed with status 128. Output \nHello from failing script"));

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());
        List<TimestampedEvent> events = testingEventAppender.getEvents();

        pipelineMustHaveFail.get(100, TimeUnit.MILLISECONDS);

        assertThat(events, hasSize(3));
        testStartStageEvent(events.get(0), "Init", null, null);
        testStageLogEvent(events.get(1), "Init", Level.INFO, "Hello from failing script");
        testEndStageEvent(events.get(2),
                "Init",
                "org.fennec.sdk.error.CancelJobException: Error during init: Exec Command [/bin/sh, src/test/resources/failing-script.sh] failed with status 128. Output \nHello from failing script",
                null);
    }

    @Test
    @SneakyThrows
    void testTimeoutException() {

        CompletableFuture<ExecCommandException> commandMustHaveFail = new CompletableFuture<>();
        CompletableFuture<Void> pipelineMustHaveFail = new CompletableFuture<>();

        Pipeline.configure(() -> {
            pipelineMustHaveFail.complete(null);
        });

        stage("Init", context -> {
            try {
                LocalExecService localExecService = new LocalExecService(System.getProperty("user.dir"), Level.INFO);
                localExecService.exec(1, "sleep", "10");
            } catch (ExecCommandException e) {
                commandMustHaveFail.complete(e);
                fail("Error during init: " + e.getMessage(), e);
            }
        });

        ExecCommandException output = commandMustHaveFail.get(1100L, TimeUnit.MILLISECONDS);
        assertThat(output.getMessage(), equalTo("Exec Command [sleep, 10] failed with Timeout after 1s"));

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());
        List<TimestampedEvent> events = testingEventAppender.getEvents();

        pipelineMustHaveFail.get(100, TimeUnit.MILLISECONDS);

        assertThat(events, hasSize(2));
        testStartStageEvent(events.get(0), "Init", null, null);
        testEndStageEvent(events.get(1),
                "Init",
                "org.fennec.sdk.error.CancelJobException: Error during init: Exec Command [sleep, 10] failed with Timeout after 1s",
                null);
    }
}
