package org.fennec.sdk.pipeline;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.fennec.sdk.model.commons.*;
import org.fennec.sdk.model.events.EndStageEvent;
import org.fennec.sdk.model.events.StageLogEvent;
import org.fennec.sdk.model.events.StartStageEvent;
import org.fennec.sdk.model.events.TimestampedEvent;
import org.fennec.sdk.testing.utils.TestingEventAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.fennec.sdk.pipeline.Pipeline.*;
import static org.fennec.sdk.testing.utils.EventTestsUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
class TestPipelineStages {

    TestingEventAppender testingEventAppender = (TestingEventAppender) ((Logger) LoggerFactory.getLogger(
            "fennec-print-events")).getAppender("STDOUT");

    private static void sleep(Long timeMs) {
        try {
            // simulate a long action
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeEach
    void init() {
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

    @Test
    void testRename() {
        rename("My new pipeline name");
        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(events, hasSize(1));
        testUpdateJobEvent(events.get(0), "My new pipeline name");
    }

    @Test
    void testFailingStage() {

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());
            assertThat(events, hasSize(2));
            testStartStageEvent(events.get(0), "Init", null, null);
            testEndStageEvent(events.get(1), "Init", "java.lang.IllegalStateException: An error occurred", null);
            completableFuture.complete(null);
        });

        stage("Init", context -> {
            throw new IllegalStateException("An error occurred");
        });

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testSingleStages() {

        stage("Init", context -> {
            log.info("A log in init");
            log.warn("A second log in init with {}", "a parameter");
        });

        stage("Build", context -> {
            log.info("A log in build");
            log.error("A second log in build with error", new IllegalStateException("An error occurred"));
        });

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(events, hasSize(8));

        testStartStageEvent(events.get(0), "Init", null, null);
        testStageLogEvent(events.get(1), "Init", Level.INFO, "A log in init");
        testStageLogEvent(events.get(2), "Init", Level.WARN, "A second log in init with a parameter");
        testEndStageEvent(events.get(3), "Init", null, null);

        testStartStageEvent(events.get(4), "Build", null, null);
        testStageLogEvent(events.get(5), "Build", Level.INFO, "A log in build");
        testStageLogEvent(events.get(6),
                "Build",
                Level.ERROR,
                "A second log in build with error\njava.lang.IllegalStateException: An error occurred\n  at org.fennec.sdk.pipeline.TestPipelineStages");
        testEndStageEvent(events.get(7), "Build", null, null);
    }

    @Test
    void testFailingParallel() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());

            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(events, hasSize(6));

            List<StartStageEvent> startStageEvents = getEventsForType(events, StartStageEvent.class);
            List<EndStageEvent> endStageEvents = getEventsForType(events, EndStageEvent.class);
            List<StageLogEvent> stageLogEvents = getEventsForType(events, StageLogEvent.class);
            assertThat(startStageEvents, hasSize(2));
            assertThat(endStageEvents, hasSize(2));
            assertThat(stageLogEvents, hasSize(2));

            // check order of execution
            // Start stage event order does not matter
            // The start stage events must be before all others events
            assertThat(events.indexOf(startStageEvents.get(0)), anyOf(equalTo(0), equalTo(1)));
            assertThat(events.indexOf(startStageEvents.get(1)), anyOf(equalTo(0), equalTo(1)));

            if (startStageEvents.get(0).getStage().equals("Sonar")) {
                testStartStageEvent(startStageEvents.get(0), "Sonar", "Sonar + Security", null);
                testStartStageEvent(startStageEvents.get(1), "Security", "Sonar + Security", null);
            } else {
                testStartStageEvent(startStageEvents.get(0), "Security", "Sonar + Security", null);
                testStartStageEvent(startStageEvents.get(1), "Sonar", "Sonar + Security", null);
            }

            // The log and end stage for Security must appear before Sonar one
            assertThat(events.get(2), equalTo(stageLogEvents.get(0)));
            assertThat(events.get(3), equalTo(endStageEvents.get(0)));
            assertThat(events.get(4), equalTo(stageLogEvents.get(1)));
            assertThat(events.get(5), equalTo(endStageEvents.get(1)));

            testStageLogEvent(events.get(2), "Security", Level.INFO, "A log in Security");
            testEndStageEvent(events.get(3), "Security", null, null);

            testStageLogEvent(events.get(4), "Sonar", Level.INFO, "A log in Sonar");
            testEndStageEvent(events.get(5), "Sonar", "java.lang.IllegalStateException: An error occurred", null);
            completableFuture.complete(null);
        });

        parallel("Sonar + Security", Map.of("Sonar", context -> {
            sleep(1000L);
            log.info("A log in Sonar");
            throw new IllegalStateException("An error occurred");
        }, "Security", context -> {
            sleep(500L);
            log.info("A log in Security");
        }));

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testParallelStages() {
        parallel("Sonar + Security", Map.of("Sonar", context -> {
            sleep(1000L);
            log.info("A log in Sonar");
        }, "Security", context -> {
            sleep(500L);
            log.info("A log in Security");
        }));

        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(events, hasSize(6));

        List<StartStageEvent> startStageEvents = getEventsForType(events, StartStageEvent.class);
        List<EndStageEvent> endStageEvents = getEventsForType(events, EndStageEvent.class);
        List<StageLogEvent> stageLogEvents = getEventsForType(events, StageLogEvent.class);
        assertThat(startStageEvents, hasSize(2));
        assertThat(endStageEvents, hasSize(2));
        assertThat(stageLogEvents, hasSize(2));

        // check order of execution
        // Start stage event order does not matter
        // The start stage events must be before all others events
        assertThat(events.indexOf(startStageEvents.get(0)), anyOf(equalTo(0), equalTo(1)));
        assertThat(events.indexOf(startStageEvents.get(1)), anyOf(equalTo(0), equalTo(1)));

        if (startStageEvents.get(0).getStage().equals("Sonar")) {
            testStartStageEvent(startStageEvents.get(0), "Sonar", "Sonar + Security", null);
            testStartStageEvent(startStageEvents.get(1), "Security", "Sonar + Security", null);
        } else {
            testStartStageEvent(startStageEvents.get(0), "Security", "Sonar + Security", null);
            testStartStageEvent(startStageEvents.get(1), "Sonar", "Sonar + Security", null);
        }

        // The log and end stage for Security must appear before Sonar one
        assertThat(events.get(2), equalTo(stageLogEvents.get(0)));
        assertThat(events.get(3), equalTo(endStageEvents.get(0)));
        assertThat(events.get(4), equalTo(stageLogEvents.get(1)));
        assertThat(events.get(5), equalTo(endStageEvents.get(1)));

        testStageLogEvent(events.get(2), "Security", Level.INFO, "A log in Security");
        testEndStageEvent(events.get(3), "Security", null, null);

        testStageLogEvent(events.get(4), "Sonar", Level.INFO, "A log in Sonar");
        testEndStageEvent(events.get(5), "Sonar", null, null);
    }

    @Test
    void testReport() {
        final TestReport testReport = TestReport
                .builder()
                .type("Unit tests")
                .suites(Arrays.asList(TestSuiteResult
                        .builder()
                        .id("Suite 1")
                        .durationMs(50L)
                        .tests(Arrays.asList(TestResult
                                        .builder()
                                        .id("Test 1")
                                        .status(TestStatus.SUCCEEDED)
                                        .durationMs(20L)
                                        .build(),
                                TestResult.builder().id("Test 2").status(TestStatus.FAILED).durationMs(20L).build()))
                        .build()))
                .build();

        stage("Unit tests", context -> {
            context.setTestResults(testReport);
        });

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());
        assertThat(events, hasSize(2));

        testStartStageEvent(events.get(0), "Unit tests", null, null);
        testEndStageEvent(events.get(1), "Unit tests", null, testReport);
    }

    @Test
    void testFailingDeployment() {

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());
            assertThat(events, hasSize(2));
            testStartStageEvent(events.get(0),
                    "Deploy to staging",
                    null,
                    new Deployment("staging", DeploymentType.LOAD));
            testEndStageEvent(events.get(1),
                    "Deploy to staging",
                    "java.lang.IllegalStateException: An error occurred",
                    null);
            completableFuture.complete(null);
        });

        deploy("staging", context -> {
            throw new IllegalStateException("An error occurred");
        });

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testDeployment() {
        deploy("staging", context -> {
            log.info("A log in staging deployment");
        });
        validateSimpleDeployment();
    }

    private void validateSimpleDeployment() {
        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());
        assertThat(events, hasSize(3));

        testStartStageEvent(events.get(0), "Deploy to staging", null, new Deployment("staging", DeploymentType.LOAD));
        testStageLogEvent(events.get(1), "Deploy to staging", Level.INFO, "A log in staging deployment");
        testEndStageEvent(events.get(2), "Deploy to staging", null, null);
    }

    @Test
    void testFailingParallelDeployment() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());

            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(events, hasSize(6));

            List<StartStageEvent> startStageEvents = getEventsForType(events, StartStageEvent.class);
            List<EndStageEvent> endStageEvents = getEventsForType(events, EndStageEvent.class);
            List<StageLogEvent> stageLogEvents = getEventsForType(events, StageLogEvent.class);
            assertThat(startStageEvents, hasSize(2));
            assertThat(endStageEvents, hasSize(2));
            assertThat(stageLogEvents, hasSize(2));

            // check order of execution
            // Start stage event order does not matter
            // The start stage events must be before all others events
            assertThat(events.indexOf(startStageEvents.get(0)), anyOf(equalTo(0), equalTo(1)));
            assertThat(events.indexOf(startStageEvents.get(1)), anyOf(equalTo(0), equalTo(1)));

            if (startStageEvents.get(0).getStage().equals("Deploy to staging (eu-west-1)")) {
                testStartStageEvent(startStageEvents.get(0),
                        "Deploy to staging (eu-west-1)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
                testStartStageEvent(startStageEvents.get(1),
                        "Deploy to staging (eu-west-2)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
            } else {
                testStartStageEvent(startStageEvents.get(0),
                        "Deploy to staging (eu-west-2)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
                testStartStageEvent(startStageEvents.get(1),
                        "Deploy to staging (eu-west-1)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
            }

            // The log and end stage for Deploy to staging (eu-west-2) must appear before Deploy to staging (eu-west-1) one
            assertThat(events.get(2), equalTo(stageLogEvents.get(0)));
            assertThat(events.get(3), equalTo(endStageEvents.get(0)));
            assertThat(events.get(4), equalTo(stageLogEvents.get(1)));
            assertThat(events.get(5), equalTo(endStageEvents.get(1)));

            testStageLogEvent(events.get(2),
                    "Deploy to staging (eu-west-2)",
                    Level.INFO,
                    "A log in staging deployment eu-west-2");
            testEndStageEvent(events.get(3), "Deploy to staging (eu-west-2)", null, null);

            testStageLogEvent(events.get(4),
                    "Deploy to staging (eu-west-1)",
                    Level.INFO,
                    "A log in staging deployment eu-west-1");
            testEndStageEvent(events.get(5),
                    "Deploy to staging (eu-west-1)",
                    "java.lang.IllegalStateException: An error occurred",
                    null);
            completableFuture.complete(null);
        });

        deploy("staging", "region", Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging deployment eu-west-1");
            throw new IllegalStateException("An error occurred");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging deployment eu-west-2");
        }));

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testParallelDeployment() {
        deploy("staging", "region", Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging deployment eu-west-1");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging deployment eu-west-2");
        }));

        validateParallelDeployment();
    }

    private void validateParallelDeployment() {
        assertThat(testingEventAppender.getUnmatched(), empty());
        assertThat(testingEventAppender.getInError(), empty());

        List<TimestampedEvent> events = testingEventAppender.getEvents();
        assertThat(events, hasSize(6));

        List<StartStageEvent> startStageEvents = getEventsForType(events, StartStageEvent.class);
        List<EndStageEvent> endStageEvents = getEventsForType(events, EndStageEvent.class);
        List<StageLogEvent> stageLogEvents = getEventsForType(events, StageLogEvent.class);
        assertThat(startStageEvents, hasSize(2));
        assertThat(endStageEvents, hasSize(2));
        assertThat(stageLogEvents, hasSize(2));

        // check order of execution
        // Start stage event order does not matter
        // The start stage events must be before all others events
        assertThat(events.indexOf(startStageEvents.get(0)), anyOf(equalTo(0), equalTo(1)));
        assertThat(events.indexOf(startStageEvents.get(1)), anyOf(equalTo(0), equalTo(1)));

        if (startStageEvents.get(0).getStage().equals("Deploy to staging (eu-west-1)")) {
            testStartStageEvent(startStageEvents.get(0),
                    "Deploy to staging (eu-west-1)",
                    "Deploy to staging",
                    new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
            testStartStageEvent(startStageEvents.get(1),
                    "Deploy to staging (eu-west-2)",
                    "Deploy to staging",
                    new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
        } else {
            testStartStageEvent(startStageEvents.get(0),
                    "Deploy to staging (eu-west-2)",
                    "Deploy to staging",
                    new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
            testStartStageEvent(startStageEvents.get(1),
                    "Deploy to staging (eu-west-1)",
                    "Deploy to staging",
                    new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
        }

        // The log and end stage for Deploy to staging (eu-west-2) must appear before Deploy to staging (eu-west-1) one
        assertThat(events.get(2), equalTo(stageLogEvents.get(0)));
        assertThat(events.get(3), equalTo(endStageEvents.get(0)));
        assertThat(events.get(4), equalTo(stageLogEvents.get(1)));
        assertThat(events.get(5), equalTo(endStageEvents.get(1)));

        testStageLogEvent(events.get(2),
                "Deploy to staging (eu-west-2)",
                Level.INFO,
                "A log in staging deployment eu-west-2");
        testEndStageEvent(events.get(3), "Deploy to staging (eu-west-2)", null, null);

        testStageLogEvent(events.get(4),
                "Deploy to staging (eu-west-1)",
                Level.INFO,
                "A log in staging deployment eu-west-1");
        testEndStageEvent(events.get(5), "Deploy to staging (eu-west-1)", null, null);
    }

    @Test
    void testDeploymentWithRollbackSuccessThenNotTriggered() {

        deploy("staging", context -> {
            log.info("A log in staging deployment");
        }, context -> {
            log.info("Rollback!!!");
        });

        validateSimpleDeployment();
    }

    @Test
    void testDeploymentWithRollbackFailedThenTriggered() {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());
            assertThat(events, hasSize(5));
            testStartStageEvent(events.get(0),
                    "Deploy to staging",
                    null,
                    new Deployment("staging", DeploymentType.LOAD));
            testEndStageEvent(events.get(1),
                    "Deploy to staging",
                    "java.lang.IllegalStateException: An error occurred",
                    null);
            testStartStageEvent(events.get(2),
                    "Rollback staging",
                    null,
                    new Deployment("staging", DeploymentType.ROLLBACK));
            testStageLogEvent(events.get(3), "Rollback staging", Level.INFO, "Rollback!!!");
            testEndStageEvent(events.get(4), "Rollback staging", null, null);
            completableFuture.complete(null);
        });

        deploy("staging", context -> {
            throw new IllegalStateException("An error occurred");
        }, context -> {
            log.info("Rollback!!!");
        });

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testParallelDeploymentWithoutSameKeyForRollback() {

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());
            assertThat(events, hasSize(2));
            testStartStageEvent(events.get(0), "Deploy to staging", null, null);
            testEndStageEvent(events.get(1),
                    "Deploy to staging",
                    "org.fennec.sdk.error.CancelJobException: Parallel rollback ([eu-west-1, eu-west-2]) must contains the same keys as Parallel deployment ([eu-west-1, us-east-1])",
                    null);
            completableFuture.complete(null);
        });

        deploy("staging", "region", Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging deployment eu-west-1");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging deployment eu-west-2");
        }), Map.of("eu-west-1", context -> {
            log.info("A log in staging rollback eu-west-1");
        }, "us-east-1", context -> {
            sleep(500L);
            log.info("A log in staging rollback eu-west-2");
        }));

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }

    @Test
    void testParallelDeploymentWithRollbackSuccessThenNotTriggered() {
        deploy("staging", "region", Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging deployment eu-west-1");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging deployment eu-west-2");
        }), Map.of("eu-west-1", context -> {
            log.info("A log in staging rollback eu-west-1");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging rollback eu-west-2");
        }));

        validateParallelDeployment();
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testParallelDeploymentWithRollbackFailedThenTriggered() {

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        Pipeline.configure(() -> {
            assertThat(testingEventAppender.getUnmatched(), empty());
            assertThat(testingEventAppender.getInError(), empty());

            List<TimestampedEvent> events = testingEventAppender.getEvents();
            assertThat(events, hasSize(12));

            List<StartStageEvent> startStageEvents = getEventsForType(events, StartStageEvent.class);
            List<EndStageEvent> endStageEvents = getEventsForType(events, EndStageEvent.class);
            List<StageLogEvent> stageLogEvents = getEventsForType(events, StageLogEvent.class);
            assertThat(startStageEvents, hasSize(4));
            assertThat(endStageEvents, hasSize(4));
            assertThat(stageLogEvents, hasSize(4));

            // check order of execution
            // Start stage event order does not matter
            // The start stage events must be before all others events
            assertThat(events.indexOf(startStageEvents.get(0)), anyOf(equalTo(0), equalTo(1)));
            assertThat(events.indexOf(startStageEvents.get(1)), anyOf(equalTo(0), equalTo(1)));

            if (startStageEvents.get(0).getStage().equals("Deploy to staging (eu-west-1)")) {
                testStartStageEvent(startStageEvents.get(0),
                        "Deploy to staging (eu-west-1)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
                testStartStageEvent(startStageEvents.get(1),
                        "Deploy to staging (eu-west-2)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
            } else {
                testStartStageEvent(startStageEvents.get(0),
                        "Deploy to staging (eu-west-2)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.LOAD));
                testStartStageEvent(startStageEvents.get(1),
                        "Deploy to staging (eu-west-1)",
                        "Deploy to staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.LOAD));
            }

            // The log and end stage for Deploy to staging (eu-west-2) must appear before Deploy to staging (eu-west-1) one
            assertThat(events.get(2), equalTo(stageLogEvents.get(0)));
            assertThat(events.get(3), equalTo(endStageEvents.get(0)));
            assertThat(events.get(4), equalTo(stageLogEvents.get(1)));
            assertThat(events.get(5), equalTo(endStageEvents.get(1)));

            testStageLogEvent(events.get(2),
                    "Deploy to staging (eu-west-2)",
                    Level.INFO,
                    "A log in staging deployment eu-west-2");
            testEndStageEvent(events.get(3), "Deploy to staging (eu-west-2)", null, null);

            testStageLogEvent(events.get(4),
                    "Deploy to staging (eu-west-1)",
                    Level.INFO,
                    "A log in staging deployment eu-west-1");
            testEndStageEvent(events.get(5),
                    "Deploy to staging (eu-west-1)",
                    "java.lang.IllegalStateException: An error occurred",
                    null);


            assertThat(events.indexOf(startStageEvents.get(2)), anyOf(equalTo(6), equalTo(7)));
            assertThat(events.indexOf(startStageEvents.get(3)), anyOf(equalTo(6), equalTo(7)));

            if (startStageEvents.get(2).getStage().equals("Rollback staging (eu-west-1)")) {
                testStartStageEvent(startStageEvents.get(2),
                        "Rollback staging (eu-west-1)",
                        "Rollback staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.ROLLBACK));
                testStartStageEvent(startStageEvents.get(3),
                        "Rollback staging (eu-west-2)",
                        "Rollback staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.ROLLBACK));
            } else {
                testStartStageEvent(startStageEvents.get(2),
                        "Rollback staging (eu-west-2)",
                        "Rollback staging",
                        new Deployment("staging", "region", "eu-west-2", DeploymentType.ROLLBACK));
                testStartStageEvent(startStageEvents.get(3),
                        "Rollback staging (eu-west-1)",
                        "Rollback staging",
                        new Deployment("staging", "region", "eu-west-1", DeploymentType.ROLLBACK));
            }

            // The log and end stage for Deploy to staging (eu-west-2) must appear before Deploy to staging (eu-west-1) one
            assertThat(events.get(8), equalTo(stageLogEvents.get(2)));
            assertThat(events.get(9), equalTo(endStageEvents.get(2)));
            assertThat(events.get(10), equalTo(stageLogEvents.get(3)));
            assertThat(events.get(11), equalTo(endStageEvents.get(3)));

            testStageLogEvent(events.get(8),
                    "Rollback staging (eu-west-2)",
                    Level.INFO,
                    "A log in staging rollback eu-west-2");
            testEndStageEvent(events.get(9), "Rollback staging (eu-west-2)", null, null);

            testStageLogEvent(events.get(10),
                    "Rollback staging (eu-west-1)",
                    Level.INFO,
                    "A log in staging rollback eu-west-1");
            testEndStageEvent(events.get(11), "Rollback staging (eu-west-1)", null, null);

            completableFuture.complete(null);
        });

        deploy("staging", "region", Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging deployment eu-west-1");
            throw new IllegalStateException("An error occurred");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging deployment eu-west-2");
        }), Map.of("eu-west-1", context -> {
            sleep(1000L);
            log.info("A log in staging rollback eu-west-1");
        }, "eu-west-2", context -> {
            sleep(500L);
            log.info("A log in staging rollback eu-west-2");
        }));

        await().atMost(200, TimeUnit.MILLISECONDS).until(completableFuture::isDone);
    }
}
