package org.fennec.sdk.exec.kube;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusCauseBuilder;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class EndOutputStream extends OutputStream {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String localMemory = "";

    @Override
    public void write(byte[] bytes) throws IOException {
        throw new IOException("Use write(int b) instead");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    public Status getStatus() {
        String rawMessage = localMemory.trim();
        log.debug("Raw status: {}", rawMessage);
        try {
            return OBJECT_MAPPER.readValue(rawMessage, Status.class);
        } catch (Exception e) {
            log.error("Unable to get Status from {}", rawMessage, e);
            return new StatusBuilder()
                    .withDetails(new StatusDetailsBuilder()
                            .withCauses(new StatusCauseBuilder().withReason("ExitCode").withMessage("1").build())
                            .build())
                    .build();
        }
    }

    @Override
    public void write(int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        localMemory = localMemory + new String(bytes);
    }
}
