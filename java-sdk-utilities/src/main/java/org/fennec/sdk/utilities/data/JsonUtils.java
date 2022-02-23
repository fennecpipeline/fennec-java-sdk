package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER_JSON = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

    @SneakyThrows
    public static String writeJSON(Object o) {
        return DataUtils.write(OBJECT_MAPPER_JSON, o);
    }

    @SneakyThrows
    public static void writeJSON(Object o, File file) {
        DataUtils.write(OBJECT_MAPPER_JSON, file, o);
    }

    @SneakyThrows
    public static JsonNode readJSON(File file) {
        return DataUtils.read(OBJECT_MAPPER_JSON, file);
    }

    @SneakyThrows
    public static JsonNode readJSON(String jsonString) {
        return DataUtils.read(OBJECT_MAPPER_JSON, jsonString);
    }

    @SneakyThrows
    public static <T> T readJSON(String jsonString, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_JSON, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readJSON(File file, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_JSON, file, type);
    }

    @SneakyThrows
    public static <T> T readJSON(String jsonString, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_JSON, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readJSON(File file, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_JSON, file, type);
    }

}
