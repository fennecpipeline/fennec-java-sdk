package org.fennec.sdk.utilities.http;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.fennec.sdk.utilities.data.JsonUtils;
import org.fennec.sdk.utilities.data.XmlUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Make HTTP Request
 */
public class Http {

    private String method;
    private String uri;
    private Map<String, List<String>> headers = new HashMap<>();
    private HttpRequest.BodyPublisher bodyPublisher;

    private Http(String method, String uri) {
        this.method = method;
        this.uri = uri;
    }

    public static Http request(String method, String uri) {
        return new Http(method, uri);
    }

    public static Http options(String uri) {
        return request("OPTIONS", uri);
    }

    public static Http head(String uri) {
        return request("HEAD", uri);
    }

    public static Http get(String uri) {
        return request("GET", uri);
    }

    public static Http post(String uri) {
        return request("POST", uri);
    }

    public static Http put(String uri) {
        return request("PUT", uri);
    }

    public static Http patch(String uri) {
        return request("PATCH", uri);
    }

    public static Http delete(String uri) {
        return request("DELETE", uri);
    }

    /**
     * Download a file
     *
     * @param url
     * @param headers
     * @param destination
     */
    @SneakyThrows
    public static void downloadFile(String url, Map<String, String> headers, File destination) {
        URL downloadUrl = new URL(url);
        HttpURLConnection myURLConnection = (HttpURLConnection) downloadUrl.openConnection();
        headers.forEach((k, v) -> {
            myURLConnection.setRequestProperty(k, v);
        });
        Files.copy(myURLConnection.getInputStream(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Add an header
     *
     * @param key
     * @param value
     * @return the HTTP instance
     */
    public Http header(String key, String value) {
        headers.computeIfAbsent(key, k -> new ArrayList<>());
        headers.get(key).add(value);
        return this;
    }

    /**
     * Add entity string (content type will be text/plain)
     *
     * @param entity the entity as string
     * @return the http
     */
    public Http entity(String entity) {
        return entity(entity, ContentTypes.TEXT_PLAIN);
    }

    /**
     * Add entity string (content type will be text/plain)
     *
     * @param entity      the entity as string
     * @param contentType the content type
     * @return the http
     */
    public Http entity(Object entity, String contentType) {
        header("Content-Type", contentType);
        if (ContentTypes.APPLICATION_JSON.equals(contentType)) {
            this.bodyPublisher = BodyPublishers.ofString(JsonUtils.writeJSON(entity));
            return this;
        } else if (ContentTypes.APPLICATION_XML.equals(contentType) || ContentTypes.TEXT_HTML.equals(contentType) || ContentTypes.APPLICATION_XHTML.equals(
                contentType)) {
            this.bodyPublisher = BodyPublishers.ofString(XmlUtils.writeXML(entity));
            return this;
        } else if (ContentTypes.TEXT_PLAIN.equals(contentType)) {
            this.bodyPublisher = BodyPublishers.ofString(entity.toString());
            return this;
        }
        throw new IllegalStateException("Unsupported media type is supported. Contributions are welcome");
    }

    /**
     * Send request and return the response
     *
     * @return the http response
     */
    public Response<String> andReturn() throws StatusCodeException, IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = makeCall();
        return new Response<>(response.body(), response.statusCode());
    }

    /**
     * @param responseBodType the response body rye
     * @param contentType     the content type expected
     * @param <T>             the type of response
     * @return the http response
     */
    public <T> Response<T> andReturn(Class<T> responseBodType, String contentType) throws StatusCodeException, IOException, InterruptedException, URISyntaxException {
        HttpResponse<String> response = makeCall();
        if (ContentTypes.APPLICATION_JSON.equals(contentType)) {
            return new Response<>(JsonUtils.readJSON(response.body(), responseBodType), response.statusCode());
        }
        if (ContentTypes.APPLICATION_XML.equals(contentType) || ContentTypes.TEXT_HTML.equals(contentType) || ContentTypes.APPLICATION_XHTML.equals(
                contentType)) {
            return new Response<>(XmlUtils.readXML(response.body(), responseBodType), response.statusCode());
        }
        throw new IllegalStateException("Unsupported media type is supported. Contributions are welcome");
    }

    /**
     * Perform call and return the {@link HttpResponse}
     *
     * @return
     */
    private HttpResponse<String> makeCall() throws StatusCodeException, IOException, InterruptedException, URISyntaxException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder builder = HttpRequest
                .newBuilder()
                .uri(new URI(uri))
                .method(method, Optional.ofNullable(bodyPublisher).orElse(BodyPublishers.noBody()));

        for (Map.Entry<String, List<String>> multiValueHeaders : headers.entrySet()) {
            for (String header : multiValueHeaders.getValue()) {
                builder = builder.header(multiValueHeaders.getKey(), header);
            }
        }
        HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new StatusCodeException(response.statusCode(), response.body());
        }
        return response;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Response<T> {
        private final T body;
        private final int status;
    }

}
