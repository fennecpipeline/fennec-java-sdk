package org.fennec.sdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static String getEnv(String name, String defaultValue) {
        String env = System.getenv(name);
        return env != null ? env : defaultValue;
    }

    /**
     * Pipeline execution folder is project/.kubepipeline
     *
     * @return
     */
    public static String getProjectFolder() {
        String userDir = System.getProperty("user.dir");
        return userDir.substring(0, userDir.lastIndexOf("/"));
    }
}
