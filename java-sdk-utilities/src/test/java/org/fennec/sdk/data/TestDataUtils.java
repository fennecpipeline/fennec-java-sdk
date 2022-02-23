package org.fennec.sdk.data;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.fennec.sdk.utilities.data.JsonUtils;
import org.fennec.sdk.utilities.data.PropertiesUtils;
import org.fennec.sdk.utilities.data.XmlUtils;
import org.fennec.sdk.utilities.data.YamlUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class TestDataUtils {

    @Test
    void testJson() {
        Translate translate = JsonUtils.readJSON(new File("src/test/resources/data/test.json"), Translate.class);
        validate(translate);
        validate(JsonUtils.readJSON(new File("src/test/resources/data/test.json")));
        assertThat(JsonUtils.writeJSON(translate), equalTo("{\"hello\":{\"french\":\"bonjour\",\"italian\":\"buongiorno\",\"spanish\":\"buenos dias\"},\"goodbye\":{\"french\":\"au revoir\",\"italian\":\"arrivederci\",\"spanish\":\"adios\"}}"));
    }

    @Test
    void testYaml() {
        Translate translate = YamlUtils.readYAML(new File("src/test/resources/data/test.yaml"), Translate.class);
        validate(translate);
        validate(YamlUtils.readYAML(new File("src/test/resources/data/test.yaml")));
        assertThat(YamlUtils.writeYAML(translate), equalTo("---\nhello:\n  french: \"bonjour\"\n  italian: \"buongiorno\"\n  spanish: \"buenos dias\"\ngoodbye:\n  french: \"au revoir\"\n  italian: \"arrivederci\"\n  spanish: \"adios\"\n"));
    }

    @Test
    void testProperties() {
        Translate translate = PropertiesUtils.readPROPERTIES(new File("src/test/resources/data/test.properties"),
                Translate.class);
        validate(translate);
        validate(PropertiesUtils.readPROPERTIES(new File("src/test/resources/data/test.properties")));
        assertThat(PropertiesUtils.writePROPERTIES(translate), equalTo("hello.french=bonjour\nhello.italian=buongiorno\nhello.spanish=buenos dias\ngoodbye.french=au revoir\ngoodbye.italian=arrivederci\ngoodbye.spanish=adios\n"));
    }

    @Test
    void testXml() {
        Translate translate = XmlUtils.readXML(new File("src/test/resources/data/test.xml"), Translate.class);
        validate(translate);
        validate(XmlUtils.readXML(new File("src/test/resources/data/test.xml")));
        assertThat(XmlUtils.writeXML(translate), equalTo("<Translate><hello><french>bonjour</french><italian>buongiorno</italian><spanish>buenos dias</spanish><japanese/></hello><goodbye><french>au revoir</french><italian>arrivederci</italian><spanish>adios</spanish><japanese/></goodbye><thankYou/></Translate>"));
    }

    private void validate(JsonNode translate) {
        assertThat(translate.get("hello").get("french").asText(), equalTo("bonjour"));
        assertThat(translate.get("hello").get("italian").asText(), equalTo("buongiorno"));
        assertThat(translate.get("hello").get("spanish").asText(), equalTo("buenos dias"));
        assertThat(translate.get("hello").get("japanese"), nullValue());
        assertThat(translate.get("goodbye").get("french").asText(), equalTo("au revoir"));
        assertThat(translate.get("goodbye").get("italian").asText(), equalTo("arrivederci"));
        assertThat(translate.get("goodbye").get("spanish").asText(), equalTo("adios"));
        assertThat(translate.get("goodbye").get("japanese"), nullValue());
        assertThat(translate.get("thankYou"), nullValue());
    }

    private void validate(Translate translate) {
        assertThat(translate.hello.french, equalTo("bonjour"));
        assertThat(translate.hello.italian, equalTo("buongiorno"));
        assertThat(translate.hello.spanish, equalTo("buenos dias"));
        assertThat(translate.hello.japanese, nullValue());
        assertThat(translate.goodbye.french, equalTo("au revoir"));
        assertThat(translate.goodbye.italian, equalTo("arrivederci"));
        assertThat(translate.goodbye.spanish, equalTo("adios"));
        assertThat(translate.goodbye.japanese, nullValue());
        assertThat(translate.thankYou, nullValue());
    }

    @Getter
    @Setter
    @JsonPropertyOrder({ "french", "italian", "spanish", "japanese" })
    private static class Translation {
        private String french;
        private String italian;
        private String spanish;
        private String japanese;
    }

    @Getter
    @Setter
    @JsonPropertyOrder({ "hello", "goodbye", "thankYou" })
    private static class Translate {
        private Translation hello;
        private Translation goodbye;
        private Translation thankYou;
    }


}
