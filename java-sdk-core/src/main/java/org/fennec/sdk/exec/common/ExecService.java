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
    CommandOutput exec(long timeoutSecond, String... cmd) throws ExecCommandException;

    /**
     * Exec a command without timeout
     *
     * @param cmd
     * @return
     * @throws ExecCommandException
     */
    CommandOutput exec(String... cmd) throws ExecCommandException;

}
