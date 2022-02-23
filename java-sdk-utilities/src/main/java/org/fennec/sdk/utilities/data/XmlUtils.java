package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

import static org.fennec.sdk.utilities.data.DataUtils.read;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class XmlUtils {

    private static final XmlMapper OBJECT_MAPPER_XML = new XmlMapper();
    ;

    @SneakyThrows
    public static String writeXML(Object o) {
        return DataUtils.write(OBJECT_MAPPER_XML, o);
    }

    @SneakyThrows
    public static void writeXML(Object o, File file) {
        DataUtils.write(OBJECT_MAPPER_XML, file, o);
    }

    @SneakyThrows
    public static JsonNode readXML(File file) {
        return read(OBJECT_MAPPER_XML, file);
    }

    @SneakyThrows
    public static JsonNode readXML(String jsonString) {
        return read(OBJECT_MAPPER_XML, jsonString);
    }

    @SneakyThrows
    public static <T> T readXML(String jsonString, Class<T> type) {
        return read(OBJECT_MAPPER_XML, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readXML(File file, Class<T> type) {
        return read(OBJECT_MAPPER_XML, file, type);
    }

    @SneakyThrows
    public static <T> T readXML(String jsonString, TypeReference<T> type) {
        return read(OBJECT_MAPPER_XML, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readXML(File file, TypeReference<T> type) {
        return read(OBJECT_MAPPER_XML, file, type);
    }

}
