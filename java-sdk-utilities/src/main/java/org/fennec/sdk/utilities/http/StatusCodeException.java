package org.fennec.sdk.utilities.http;

import lombok.Getter;

@Getter
public class StatusCodeException extends Exception {

    private static final long serialVersionUID = 1L;

    private int statusCode;

    private String payload;

    public StatusCodeException(int statusCode, String payload) {
        super("Bad status code received: " + statusCode);
        this.statusCode = statusCode;
        this.payload = payload;
    }

}
