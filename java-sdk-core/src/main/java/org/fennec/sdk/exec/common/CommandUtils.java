package org.fennec.sdk.exec.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandUtils {

    public static String[] command(String mainCommand, String... options) throws ExecCommandException {
        return Stream
                .concat(Stream.of(mainCommand), Optional.ofNullable(options).stream().flatMap(Arrays::stream))
                .toArray(String[]::new);
    }

    public static String[] command(String mainCommand, String child1, String... options) throws ExecCommandException {
        return Stream
                .concat(Stream.of(mainCommand, child1), Optional.ofNullable(options).stream().flatMap(Arrays::stream))
                .toArray(String[]::new);
    }

    public static String[] command(String mainCommand, String child1, String child2,
            String... options) throws ExecCommandException {
        return Stream
                .concat(Stream.of(mainCommand, child1, child2),
                        Optional.ofNullable(options).stream().flatMap(Arrays::stream))
                .toArray(String[]::new);
    }

    public static String[] command(String mainCommand, String child1, String child2, String child3,
            String... options) throws ExecCommandException {
        return Stream
                .concat(Stream.of(mainCommand, child1, child2, child3),
                        Optional.ofNullable(options).stream().flatMap(Arrays::stream))
                .toArray(String[]::new);
    }
}
