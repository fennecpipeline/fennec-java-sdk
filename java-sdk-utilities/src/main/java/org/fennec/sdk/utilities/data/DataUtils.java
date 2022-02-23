package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class DataUtils {

    @SneakyThrows
    static <T> T read(ObjectMapper mapper, String jsonString, Class<T> type) {
        return mapper.readValue(jsonString, type);
    }

    @SneakyThrows
    static <T> T read(ObjectMapper mapper, File file, Class<T> type) {
        return mapper.readValue(file, type);
    }

    @SneakyThrows
    static JsonNode read(ObjectMapper mapper, String jsonString) {
        return mapper.readTree(jsonString);
    }

    @SneakyThrows
    static JsonNode read(ObjectMapper mapper, File file) {
        return mapper.readTree(file);
    }

    @SneakyThrows
    public static <T> T read(ObjectMapper mapper, String jsonString, TypeReference<T> type) {
        return mapper.readValue(jsonString, type);
    }

    @SneakyThrows
    public static <T> T read(ObjectMapper mapper, File file, TypeReference<T> type) {
        return mapper.readValue(file, type);
    }

    @SneakyThrows
    static String write(ObjectMapper mapper, Object o) {
        return mapper.writeValueAsString(o);
    }

    @SneakyThrows
    static void write(ObjectMapper mapper, File file, Object o) {
        mapper.writeValue(file, o);
        ;
    }
}
