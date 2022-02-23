package org.fennec.sdk.exec.local;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.fennec.sdk.exec.common.*;
import org.fennec.sdk.utils.Utils;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Execute actions locally (or on same container)
 */
@RequiredArgsConstructor
public class LocalExecService implements ExecService {

    private final String location;

    private final Level level;

    public LocalExecService() {
        this.location = Utils.getProjectFolder();
        this.level = Level.INFO;
    }

    public static CommandOutput exec(String... cmd) throws ExecCommandException {
        return new LocalExecService().execCommand(cmd);
    }

    public static CommandOutput exec(long timeoutSecond, String... cmd) throws ExecCommandException {
        return new LocalExecService().execCommand(timeoutSecond, cmd);
    }

    @Override
    public CommandOutput execCommand(long timeoutSecond, String... cmd) throws ExecCommandException {
        return executeCommand(timeoutSecond, cmd);
    }

    @Override
    public CommandOutput execCommand(String... cmd) throws ExecCommandException {
        return executeCommand(null, cmd);
    }

    private CommandOutput executeCommand(Long timeoutSecond, String... cmd) throws ExecCommandException {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(cmd).redirectErrorStream(true).directory(new File(location));
            Process process = builder.start();
            if (timeoutSecond == null) {
                return monitorProcess(process, cmd);
            }
            return monitorProcessWithTimeout(timeoutSecond, process, cmd);
        } catch (IOException e) {
            throw new ExecCommandException(cmd, e);
        }
    }

    private CommandOutput monitorProcessWithTimeout(Long timeoutSecond, Process process,
            String[] cmd) throws ExecCommandException {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return monitorProcess(process, cmd);
                } catch (ExecCommandException e) {
                    throw new ExecAsyncCommandException(e);
                }
            }).get(timeoutSecond, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if  (e.getCause() instanceof  ExecAsyncCommandException) {
                throw ((ExecAsyncCommandException) e.getCause()).getCause();
            }
            throw new ExecCommandException(cmd, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecCommandException(cmd, e);
        } catch (TimeoutException e) {
            // In case of timeout destroy the process
            process.destroy();
            throw new ExecCommandException(cmd, String.format("Timeout after %ds", timeoutSecond), e);
        }
    }

    @SneakyThrows
    private CommandOutput monitorProcess(Process process, String... cmd) throws ExecCommandException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(cmd).redirectErrorStream(true).directory(new File(location));
        try (LogOutputStream logOutputStream = new LogOutputStream(level, MDC.getCopyOfContextMap())){
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(reader);
            String line;

            while ((line = br.readLine()) != null) {
                logOutputStream.write(line.getBytes());
                logOutputStream.write("\n".getBytes());
            }

            process.waitFor();

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                ExecCommandException exception = new ExecCommandException(cmd, exitCode, logOutputStream.toString());
                logOutputStream.close();
                throw exception;
            }

            CommandOutput commandOutput = new CommandOutput(exitCode, logOutputStream.toString());
            logOutputStream.close();
            return commandOutput;
        } catch (IOException e) {
            throw new ExecCommandException(cmd, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecCommandException(cmd, e);
        }
    }
}
