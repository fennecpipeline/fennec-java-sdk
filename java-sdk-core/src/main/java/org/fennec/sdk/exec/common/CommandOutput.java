package org.fennec.sdk.exec.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CommandOutput {

    private final int status;

    private final String data;

}
