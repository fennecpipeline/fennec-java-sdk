package org.fennec.sdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static String env(String name, String defaultValue) {
        return env(name).orElse(defaultValue);
    }

    public static Optional<String> env(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    /**
     * Pipeline execution folder is project/.kubepipeline
     * @return the project folder
     */
    public static String getProjectFolder() {
        String userDir = System.getProperty("user.dir");
        return userDir.substring(0, userDir.lastIndexOf("/"));
    }
}
