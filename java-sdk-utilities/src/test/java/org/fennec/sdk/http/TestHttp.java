package org.fennec.sdk.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.*;
import org.apache.commons.io.FileUtils;
import org.fennec.sdk.utilities.http.ContentTypes;
import org.fennec.sdk.utilities.http.Http;
import org.fennec.sdk.utilities.http.StatusCodeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.fennec.sdk.utilities.http.Http.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class TestHttp {

    private static WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().notifier(new ConsoleNotifier(
            true)));

    @BeforeAll
    static void startWiremockServer() {
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
    }

    @BeforeEach
    void init() {
        wireMockServer.resetAll();
    }

    @Test
    @SneakyThrows
    void testStatus401() {

        WireMock.stubFor(WireMock.options(WireMock.urlPathMatching("/test")).willReturn(WireMock.aResponse().withBody("Not authorized").withStatus(401)));

        try {
            options("http://localhost:8080/test").andReturn();
            Assertions.fail("Exception must have been raised");
        } catch (StatusCodeException e) {
            assertThat(e.getStatusCode(), equalTo(401));
            assertThat(e.getPayload(), equalTo("Not authorized"));
        }
    }

    @Test
    @SneakyThrows
    void testOptions() {

        WireMock.stubFor(WireMock.options(WireMock.urlPathMatching("/test")).willReturn(WireMock.aResponse().withStatus(200)));

        Http.Response<String> response = options("http://localhost:8080/test").andReturn();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getBody(), equalTo(""));
    }

    @Test
    @SneakyThrows
    void testHead() {

        WireMock.stubFor(WireMock.head(WireMock.urlPathMatching("/test")).willReturn(WireMock.aResponse().withStatus(200)));

        Http.Response<String> response = head("http://localhost:8080/test").andReturn();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getBody(), equalTo(""));
    }

    @Test
    @SneakyThrows
    void testDelete() {

        WireMock.stubFor(WireMock.delete(WireMock.urlPathMatching("/test")).willReturn(WireMock.aResponse().withStatus(204)));

        Http.Response<String> response = delete("http://localhost:8080/test").andReturn();

        assertThat(response.getStatus(), equalTo(204));
        assertThat(response.getBody(), equalTo(""));
    }

    @Test
    @SneakyThrows
    void testGet() {

        WireMock.stubFor(WireMock.get("/test?q=hello")
                .withHeader("Content-Type", WireMock.equalTo("text/plain"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("Hello world!")));

        Http.Response<String> response = get("http://localhost:8080/test?q=hello").header("Content-Type", "text/plain").andReturn();
        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getBody(), equalTo("Hello world!"));
    }

    @Test
    @SneakyThrows
    void testPostJson() {

        WireMock.stubFor(WireMock.post("/test")
                .withHeader("Content-Type", WireMock.equalTo(ContentTypes.APPLICATION_JSON))
                .withRequestBody(WireMock.equalToJson("{\"name\":\"John\"}"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", ContentTypes.APPLICATION_JSON)
                        .withBody("{\"hello\":\"John\"}")));

        Response<Hello> response = post("http://localhost:8080/test")
                .entity(new Name("John"), ContentTypes.APPLICATION_JSON)
                .andReturn(Hello.class, ContentTypes.APPLICATION_JSON);

        assertThat(response.getStatus(), equalTo(201));
        assertThat(response.getBody(), equalTo(new Hello("John")));
    }

    @Test
    @SneakyThrows
    void testPutXml() {

        WireMock.stubFor(WireMock.put("/test")
                .withHeader("Content-Type", WireMock.equalTo(ContentTypes.APPLICATION_XML))
                .withRequestBody(WireMock.equalToXml("<Name><name>John</name></Name>"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", ContentTypes.APPLICATION_XML)
                        .withBody("<hello>John</hello>")));

        Response<Hello> response = put("http://localhost:8080/test")
                .entity(new Name("John"), ContentTypes.APPLICATION_XML)
                .andReturn(Hello.class, ContentTypes.APPLICATION_XML);

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getBody(), equalTo(new Hello("John")));
    }

    @Test
    @SneakyThrows
    void testPatchText() {

        WireMock.stubFor(WireMock.patch(WireMock.urlPathMatching("/test"))
                .withHeader("Content-Type", WireMock.equalTo(ContentTypes.TEXT_PLAIN))
                .withRequestBody(WireMock.equalTo("Name: John"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("Hello John")));

        Response<String> response = patch("http://localhost:8080/test?q=hello")
                .entity("Name: John")
                .andReturn();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getBody(), equalTo("Hello John"));
    }

    @Test
    @SneakyThrows
    void testDownloadFile() {

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/test"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("Hello John")));

        File destination = new File("download.txt");
        destination.deleteOnExit();

        downloadFile("http://localhost:8080/test", Map.of("Authorization", "Bearer token"), destination);

        assertThat(destination.exists(), equalTo(true));
        assertThat(FileUtils.readFileToString(destination), equalTo("Hello John"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    static class Hello {
        private String hello;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    static class Name {
        private String name;
    }
}
