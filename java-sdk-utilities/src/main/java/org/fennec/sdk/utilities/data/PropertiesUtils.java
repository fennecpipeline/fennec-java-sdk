package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PropertiesUtils {

    private static final ObjectMapper OBJECT_MAPPER_PROPERTIES = new ObjectMapper(new JavaPropsFactory()).setSerializationInclusion(
            Include.NON_NULL);

    @SneakyThrows
    public static String writePROPERTIES(Object o) {
        return DataUtils.write(OBJECT_MAPPER_PROPERTIES, o);
    }

    @SneakyThrows
    public static void writePROPERTIES(Object o, File file) {
        DataUtils.write(OBJECT_MAPPER_PROPERTIES, file, o);
    }

    @SneakyThrows
    public static JsonNode readPROPERTIES(File file) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, file);
    }

    @SneakyThrows
    public static JsonNode readPROPERTIES(String jsonString) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, jsonString);
    }

    @SneakyThrows
    public static <T> T readPROPERTIES(String jsonString, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readPROPERTIES(File file, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, file, type);
    }

    @SneakyThrows
    public static <T> T readPROPERTIES(String jsonString, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readPROPERTIES(File file, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_PROPERTIES, file, type);
    }

}
