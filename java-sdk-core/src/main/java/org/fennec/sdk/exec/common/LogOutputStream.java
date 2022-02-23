package org.fennec.sdk.exec.common;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @see <a href="https://stackoverflow.com/questions/6995946/log4j-how-do-i-redirect-an-outputstream-or-writer-to-loggers-writers">Thanks Arthur</a>
 */
@Slf4j
public class LogOutputStream extends OutputStream {

    private final Level level;

    private final Map<String, String> mdcContext;

    /**
     * The internal memory for the written bytes.
     */
    private String localMemory = "";

    private StringBuilder allLogs = new StringBuilder();

    public LogOutputStream(Level level, Map<String, String> mdcContext) {
        this.level = level;
        this.mdcContext = mdcContext;
    }

    @Override
    public void write(byte[] bytes) {
        localMemory = localMemory + new String(bytes);

        if (localMemory.endsWith("\n")) {
            localMemory = localMemory.trim();
            flush();
        }
    }

    /**
     * Writes a byte to the output stream. This method flushes automatically at the end of a line.
     */
    @Override
    public void write(int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        write(bytes);
    }

    /**
     * Flushes the output stream.
     */
    @Override
    public void flush() {
        if (localMemory.isEmpty()) {
            return;
        }
        // To stay in the same context set the context map
        MDC.setContextMap(mdcContext);
        switch (level) {
            case TRACE:
                log.trace(localMemory);
                break;
            case DEBUG:
                log.debug(localMemory);
                break;
            case INFO:
                log.info(localMemory);
                break;
            case WARN:
                log.warn(localMemory);
                break;
            case ERROR:
                log.error(localMemory);
                break;
            default:
                break;
        }
        allLogs.append(localMemory).append("\n");
        localMemory = "";
    }

    @Override
    public String toString() {
        return Optional.ofNullable(allLogs).map(StringBuilder::toString).map(String::trim).orElse("");
    }

    @Override
    public void close() throws IOException {
        super.close();
        localMemory = null;
        allLogs = null;
    }
}
