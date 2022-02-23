package org.fennec.sdk.error;

public class CancelJobException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public CancelJobException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelJobException(String message) {
        super(message);
    }

}
