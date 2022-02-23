package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

import static org.fennec.sdk.utilities.data.DataUtils.read;
import static org.fennec.sdk.utilities.data.DataUtils.write;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class YamlUtils {

    private static final ObjectMapper OBJECT_MAPPER_YAML = new ObjectMapper(new YAMLFactory()).setSerializationInclusion(
            Include.NON_NULL);

    @SneakyThrows
    public static String writeYAML(Object o) {
        return DataUtils.write(OBJECT_MAPPER_YAML, o);
    }

    @SneakyThrows
    public static void writeYAML(Object o, File file) {
        DataUtils.write(OBJECT_MAPPER_YAML, file, o);
    }

    @SneakyThrows
    public static JsonNode readYAML(File file) {
        return DataUtils.read(OBJECT_MAPPER_YAML, file);
    }

    @SneakyThrows
    public static JsonNode readYAML(String jsonString) {
        return DataUtils.read(OBJECT_MAPPER_YAML, jsonString);
    }

    @SneakyThrows
    public static <T> T readYAML(String jsonString, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_YAML, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readYAML(File file, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_YAML, file, type);
    }

    @SneakyThrows
    public static <T> T readYAML(String jsonString, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_YAML, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readYAML(File file, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_YAML, file, type);
    }

}
