package org.fennec.sdk.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtils {

    public static final String SECRET = "secret.";
    private static final String CONCEALMENT = "******";

    public static String getFormattedMessage(ILoggingEvent event) {
        return appendStackTraceIfNeeded(concealMessage(event.getFormattedMessage(), event.getMDCPropertyMap()),
                event.getThrowableProxy())
                // Remove colorization
                .replaceAll("\\u001b\\[.*?m", "");
    }

    /**
     * Reformat the logs as it does not provide direct link to {@link Throwable}
     *
     * @param throwableProxy
     * @return the formatted stack trace
     */
    private static String appendStackTraceIfNeeded(String message, IThrowableProxy throwableProxy) {
        if (throwableProxy == null || throwableProxy.getClass() == null) {
            return message;
        }
        return String.format("%s%n%s", message, getStackTraceString(throwableProxy));
    }

    private static String getStackTraceString(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return "";
        }
        return new StringBuilder()
                .append(throwableProxy.getClassName())
                .append(": ")
                .append(throwableProxy.getMessage())
                .append("\n")
                .append(Arrays
                        .asList(throwableProxy.getStackTraceElementProxyArray())
                        .stream()
                        .map(StackTraceElementProxy::getSTEAsString)
                        .map(element -> "  " + element)
                        .collect(Collectors.joining("\n")))
                .append(getStackTraceString(throwableProxy.getCause()))
                .toString();
    }

    private static String concealMessage(String message, Map<String, String> map) {
        return concealMessage(message,
                map
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith(SECRET))
                        .map(Entry::getValue)
                        .collect(Collectors.toList()));
    }

    /**
     * @param message
     * @param toConceal
     * @return the conceleaded message
     */
    public static String concealMessage(String message, List<String> toConceal) {
        if (toConceal == null) {
            return message;
        }
        for (String conceal : toConceal) {
            while (message.indexOf(conceal) != -1) {
                message = message.replace(conceal, CONCEALMENT);
            }
        }
        return message;
    }

}
