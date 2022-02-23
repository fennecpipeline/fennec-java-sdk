package org.fennec.sdk.exec.common;

public interface ExecService {

    /**
     * Exec a command
     *
     * @param timeoutSecond
     * @param cmd
     * @return
     * @throws ExecCommandException
     */
    CommandOutput execCommand(long timeoutSecond, String... cmd) throws ExecCommandException;

    /**
     * Exec a command without timeout
     *
     * @param cmd
     * @return
     * @throws ExecCommandException
     */
    CommandOutput execCommand(String... cmd) throws ExecCommandException;

}
