package org.fennec.sdk.pipeline.model;

public class FailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FailException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailException(String message) {
        super(message);
    }

}
