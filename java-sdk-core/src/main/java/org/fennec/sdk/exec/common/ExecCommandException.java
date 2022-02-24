package org.fennec.sdk.exec.common;

import lombok.Getter;
import org.fennec.sdk.error.CancelJobException;

import java.util.Arrays;

@Getter
public class ExecCommandException extends CancelJobException {

    private static final long serialVersionUID = 1L;

    private final String[] command;

    private final int statusCode;

    private final String output;

    public ExecCommandException(String[] command, Throwable cause) {
        super(String.format("Exec Command %s failed with error", Arrays.toString(command)), cause);
        this.command = command;
        this.statusCode = -1;
        this.output = null;
    }

    public ExecCommandException(String[] command, String message) {
        super(String.format("Exec Command %s failed with %s", Arrays.toString(command), message));
        this.command = command;
        this.statusCode = -1;
        this.output = null;
    }

    public ExecCommandException(String[] command, String message, Throwable cause) {
        super(String.format("Exec Command %s failed with %s", Arrays.toString(command), message), cause);
        this.command = command;
        this.statusCode = -1;
        this.output = null;
    }

    public ExecCommandException(String[] command, int statusCode, String output) {
        super(String.format("Exec Command %s failed with status %d. Output %n%s",
                Arrays.toString(command),
                statusCode,
                output));
        this.command = command;
        this.statusCode = statusCode;
        this.output = output;
    }

}
