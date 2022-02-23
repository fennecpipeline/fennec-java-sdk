package org.fennec.sdk.utilities.data;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CSVUtils {

    private static final ObjectMapper OBJECT_MAPPER_CSV = new ObjectMapper(new CsvFactory()).setSerializationInclusion(
            Include.NON_NULL);

    @SneakyThrows
    public static String writeCSV(Object o) {
        return DataUtils.write(OBJECT_MAPPER_CSV, o);
    }

    @SneakyThrows
    public static void writeCSV(Object o, File file) {
        DataUtils.write(OBJECT_MAPPER_CSV, file, o);
    }

    @SneakyThrows
    public static JsonNode readCSV(File file) {
        return DataUtils.read(OBJECT_MAPPER_CSV, file);
    }

    @SneakyThrows
    public static JsonNode readCSV(String jsonString) {
        return DataUtils.read(OBJECT_MAPPER_CSV, jsonString);
    }

    @SneakyThrows
    public static <T> T readCSV(String jsonString, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_CSV, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readCSV(File file, Class<T> type) {
        return DataUtils.read(OBJECT_MAPPER_CSV, file, type);
    }

    @SneakyThrows
    public static <T> T readCSV(String jsonString, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_CSV, jsonString, type);
    }

    @SneakyThrows
    public static <T> T readCSV(File file, TypeReference<T> type) {
        return DataUtils.read(OBJECT_MAPPER_CSV, file, type);
    }

}
