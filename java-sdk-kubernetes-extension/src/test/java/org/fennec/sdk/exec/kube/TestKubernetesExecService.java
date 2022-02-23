package org.fennec.sdk.exec.kube;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.http.WebSocket;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.server.mock.OutputStreamMessage;
import io.fabric8.mockwebserver.internal.WebSocketMessage;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.fennec.sdk.error.Fail.fail;
import static org.fennec.sdk.pipeline.Pipeline.stage;
import static org.fennec.sdk.testing.utils.EventTestsUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@EnableKubernetesMockClient(crud = false)
public class TestKubernetesExecService {

    /**
     * This end stream id allows to close the with a {@link io.fabric8.kubernetes.api.model.Status}
     * Link: {@link io.fabric8.kubernetes.client.dsl.internal.ExecWebSocketListener#onMessage(WebSocket, String)}
     */
    private static final byte END_STREAM_ID = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    TestingEventAppender testingEventAppender = (TestingEventAppender) ((Logger) LoggerFactory.getLogger(
            "fennec-print-events")).getAppender("STDOUT");
    KubernetesMockServer server;
    KubernetesClient client;

    /**
     * @param prefix the prefix
     * @param body   the body
     * @return a formatted body bytes prefixed with the STREAM_ID ({@link io.fabric8.kubernetes.client.dsl.internal.ExecWebSocketListener#onMessage(WebSocket, String)})
     */
    private static byte[] getBodyBytes(byte prefix, String body) {
        byte[] original = body.getBytes(StandardCharsets.UTF_8);
        byte[] prefixed = new byte[original.length + 1];
        prefixed[0] = prefix;
        System.arraycopy(original, 0, prefixed, 1, original.length);
        return prefixed;
    }

    /**
     * @param parameterValue the parameter
     * @return the encoded parameter
     * @throws IllegalStateException
     */
    private static final String getUrlEncodedParameter(String parameterValue) {
        try {
            return URLEncoder.encode(parameterValue, "utf-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param command the command to execute
     * @return the formatted (with url encoded parameters)
     */
    private static final String toCommand(String command) {
        return "command=" + getUrlEncodedParameter(command);
    }

    /**
     * @param pod       the targeted pod
     * @param container the targeted container
     * @param command   the command fragments
     * @return the formatted uri
     */
    private static final String formatUri(String pod, String container, List<String> command) {
        return "/api/v1/namespaces/test/pods/" + pod + "/exec?" + command
                .stream()
                .map(TestKubernetesExecService::toCommand)
                .collect(Collectors.joining("&")) + "&container=" + container + "&stdout=true&stderr=true";
    }

    /**
     * @param statusCode the exit code
     * @return a {@link io.fabric8.kubernetes.api.model.Status} to string
     */
    @SneakyThrows
    private static final String statusToString(int statusCode) {
        if (statusCode != 0) {
            return OBJECT_MAPPER.writeValueAsString(new StatusBuilder()
                    .withStatus("Failed")
                    .withDetails(new StatusDetailsBuilder()
                            .withCauses(new StatusCauseBuilder()
                                    .withReason("ExitCode")
                                    .withMessage(String.valueOf(statusCode))
                                    .build())
                            .build())
                    .build());
        }
        return OBJECT_MAPPER.writeValueAsString(new StatusBuilder().withStatus("Success").build());
    }

    /**
     * @param statusCode the status code
     * @return the status as byte array
     */
    private static final byte[] status(int statusCode) {
        return getBodyBytes(END_STREAM_ID, statusToString(statusCode));
    }

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

    @Test
    @SneakyThrows
    public void testSuccess() {

        server
                .expect()
                .get()
                .withPath("/api/v1/namespaces/test/pods/test")
                .andReturn(200,
                        new PodBuilder().withMetadata(new ObjectMetaBuilder()
                                .withName("test")
                                .withNamespace("test")
                                .build()).build())
                .always();

        server
                .expect()
                .get()
                .withPath(formatUri("test", "test-container", Arrays.asList("echo", "Hello\nworld")))
                .andUpgradeToWebSocket()
                .open()
                .waitFor(5L)
                .andEmit(new OutputStreamMessage("Hello\nworld"))
                .waitFor(10L)
                .andEmit(new WebSocketMessage(10L, status(0), false))
                .done()
                .once();

        CompletableFuture<CommandOutput> completableFuture = new CompletableFuture<>();

        stage("Init", context -> {
            try {
                KubernetesExecService kubernetesExecService = new KubernetesExecService(client,
                        "test",
                        "test",
                        "test-container",
                        200L);
                CommandOutput output = kubernetesExecService.execCommand("echo", "Hello\nworld");
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
        assertThat(events, hasSize(7));
        testStartStageEvent(events.get(0), "Init", null, null);
        testStageLogEvent(events.get(1), "Init", Level.DEBUG, "Running command: [echo, Hello\n" + "world] on pod test in namespace test");
        testStageLogEvent(events.get(2), "Init", Level.INFO, "Hello");
        testStageLogEvent(events.get(3), "Init", Level.INFO, "world");
        testStageLogEvent(events.get(4), "Init", Level.DEBUG, "Exec Web Socket: On Close with code:[1000], due to: [Closing...]");
        testStageLogEvent(events.get(5), "Init", Level.DEBUG, "Raw status: {\"apiVersion\":\"v1\",\"kind\":\"Status\",\"status\":\"Success\"}");
        testEndStageEvent(events.get(6), "Init", null, null);
    }

    @Test
    @SneakyThrows
    public void testFailure() {

        server
                .expect()
                .get()
                .withPath("/api/v1/namespaces/test/pods/test")
                .andReturn(200,
                        new PodBuilder().withMetadata(new ObjectMetaBuilder()
                                .withName("test")
                                .withNamespace("test")
                                .build()).build())
                .always();

        server
                .expect()
                .get()
                .withPath(formatUri("test", "test-container", Arrays.asList("dhohc", "uhovueh")))
                .andUpgradeToWebSocket()
                .open()
                .waitFor(5L)
                .andEmit(new OutputStreamMessage("Command dhohc dhohc not found"))
                .waitFor(10L)
                .andEmit(new WebSocketMessage(10L, status(128), false))
                .done()
                .once();

        CompletableFuture<ExecCommandException> completableFuture = new CompletableFuture<>();

        stage("Init", context -> {
            try {
                KubernetesExecService kubernetesExecService = new KubernetesExecService(client,
                        "test",
                        "test",
                        "test-container",
                        200L);
                kubernetesExecService.execCommand("dhohc", "uhovueh");
                fail("Command must fail");
            } catch (ExecCommandException e) {
                completableFuture.complete(e);
            }
        });

        ExecCommandException output = completableFuture.get(200, TimeUnit.MILLISECONDS);

        assertThat(output.getStatusCode(), equalTo(128));
        assertThat(output.getOutput(), equalTo("Command dhohc dhohc not found"));
    }
}
