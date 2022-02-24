package org.fennec.sdk.exec.common;

import lombok.Getter;

public class ExecAsyncCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final ExecCommandException execCommandException;

    public ExecAsyncCommandException(ExecCommandException cause) {
        super(cause);
        this.execCommandException = cause;
    }
}
