package org.fennec.sdk.error;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Fail {

    public static void fail(String message) {
        throw new CancelJobException(message);
    }

    public static void fail(String message, Throwable t) {
        throw new CancelJobException(message, t);
    }

}
