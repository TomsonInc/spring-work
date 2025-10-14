package itx.examples.springboot.proxy.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProxyTestsIT {

    private static final String BASE_URI = "http://localhost:8088/proxy";
    private static HttpClient httpClient;

    @BeforeAll
    public static void setUp() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Test
    @DisplayName("Should return 200 OK for /proxy/info")
    public void testValidProxyCall() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/info");

        assertNotNull(response, "Response should not be null");
        assertEquals(200, response.statusCode(), "Expected HTTP 200 for valid endpoint");

        // Optional: Print response body for debugging
        System.out.println("Response body (valid call): " + response.body());
    }

    @Test
    @DisplayName("Should return 404 Not Found for /proxy/non-existing-endpoint")
    public void testInvalidProxyCall() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/non-existing-endpoint");

        assertNotNull(response, "Response should not be null");
        assertEquals(404, response.statusCode(), "Expected HTTP 404 for invalid endpoint");

        // Optional: Print response body for debugging
        System.out.println("Response body (invalid call): " + response.body());
    }

    /**
     * Sends a GET request to the given relative URI and returns the response.
     *
     * @param path path relative to base URI, e.g. "/info"
     * @return HttpResponse<String>
     * @throws IOException, InterruptedException
     */
    private HttpResponse<String> sendGetRequest(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URI + path))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
