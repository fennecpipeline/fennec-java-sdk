package org.fennec.sdk.exec.common;

import java.util.Arrays;

public class ExecAsyncCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ExecCommandException cause;

    public ExecAsyncCommandException(ExecCommandException cause) {
        this.cause = cause;
    }

    @Override
    public ExecCommandException getCause() {
        return cause;
    }
}
