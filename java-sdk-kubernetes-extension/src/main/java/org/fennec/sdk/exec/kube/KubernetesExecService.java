package org.fennec.sdk.exec.kube;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fennec.sdk.exec.common.CommandOutput;
import org.fennec.sdk.exec.common.ExecCommandException;
import org.fennec.sdk.exec.common.ExecService;
import org.fennec.sdk.exec.common.LogOutputStream;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Exec actions on a pod/container. The correct SA must be attached.
 */
@Slf4j
@RequiredArgsConstructor
public class KubernetesExecService implements ExecService {

    private static final String SUCCESS = "Success";

    private final KubernetesClient client;

    private final String namespace;

    private final String podName;

    private final String container;

    private final long defaultTimeoutSeconds;

    public KubernetesExecService(String namespace, String podName, String container, long defaultTimeoutSeconds) {
        this.client = KubernetesClientProducer.getClient();
        this.namespace = namespace;
        this.podName = podName;
        this.container = container;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public KubernetesExecService(String namespace, String podName, long defaultTimeoutSeconds) {
        this.client = KubernetesClientProducer.getClient();
        this.namespace = namespace;
        this.podName = podName;
        this.container = null;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    @Override
    public CommandOutput execCommand(String... cmd) throws ExecCommandException {
        return execCommand(defaultTimeoutSeconds, cmd);
    }

    /**
     * Execute provided command in provided pod and container
     *
     * @param timeoutSecond
     * @param cmd
     * @return
     * @throws ExecCommandException
     */
    @Override
    public CommandOutput execCommand(long timeoutSecond, String... cmd) throws ExecCommandException {
        Pod pod = client.pods().inNamespace(namespace).withName(podName).get();
        log.debug("Running command: {} on pod {} in namespace {}",
                Arrays.toString(cmd),
                pod.getMetadata().getName(),
                namespace);

        CompletableFuture<CommandOutput> data = new CompletableFuture<>();
        try (ExecWatch execWatch = execCmd(pod, data, cmd)) {
            try {
                return data.get(timeoutSecond, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                if (e.getCause() instanceof ExecCommandException) {
                    throw (ExecCommandException) e.getCause();
                }
                throw new ExecCommandException(cmd, e);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                throw new ExecCommandException(cmd, e1);
            }
        }
    }

    private ExecWatch execCmd(Pod pod, CompletableFuture<CommandOutput> data, String... command) {
        Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
        LogOutputStream logOutputStream = new LogOutputStream(Level.INFO, mdcContextMap);
        EndOutputStream endOutputStream = new EndOutputStream();
        return client
                .pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .inContainer(container)
                .writingOutput(logOutputStream)
                .writingError(logOutputStream)
                .writingErrorChannel(endOutputStream)
                .usingListener(new SimpleListener(data, logOutputStream, endOutputStream, mdcContextMap, command))
                .exec(command);
    }

    @RequiredArgsConstructor
    static class SimpleListener implements ExecListener {

        private final CompletableFuture<CommandOutput> data;
        private final LogOutputStream logOutputStream;
        private final EndOutputStream endOutputStream;
        private final Map<String, String> mdcContextMap;
        private final String[] cmd;

        @Override
        public void onOpen() {
            MDC.setContextMap(mdcContextMap);
        }

        @Override
        public void onFailure(Throwable t, Response response) {
            MDC.setContextMap(mdcContextMap);
            try {
                log.error(t.getMessage() + " " + response.body());
            } catch (IOException e) {
                // DO NOTHING
            }
            data.completeExceptionally(t);
        }

        @Override
        public void onClose(int code, String reason) {
            MDC.setContextMap(mdcContextMap);
            Status status = endOutputStream.getStatus();
            if (status.getMessage() != null) {
                logOutputStream.write(status.getMessage().getBytes());
            }

            int exitCode = getStatusCode(status);

            if (exitCode != 0) {
                ExecCommandException exception = new ExecCommandException(cmd, exitCode, logOutputStream.toString());
                closeStreams();
                data.completeExceptionally(exception);
                return;
            }

            data.complete(new CommandOutput(exitCode, logOutputStream.toString()));
            closeStreams();
        }

        private void closeStreams() {
            try {
                logOutputStream.close();
                endOutputStream.close();
            } catch (IOException e) {
                log.error("Cannot close streams", e);
            }
        }

        private int getStatusCode(Status status) {
            if (SUCCESS.equals(status.getStatus())) {
                return 0;
            }
            return Optional
                    .ofNullable(status.getDetails())
                    .map(StatusDetails::getCauses)
                    .stream()
                    .flatMap(List::stream)
                    .filter(cause -> cause.getReason() != null && cause.getReason().equals("ExitCode"))
                    .findFirst()
                    .map(StatusCause::getMessage)
                    .map(Integer::valueOf)
                    .orElse(1);
        }
    }
}
