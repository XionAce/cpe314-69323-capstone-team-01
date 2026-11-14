package edu.uc.campusmarket.api;

import edu.uc.campusmarket.dto.CatalogResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/** Downloads the public catalog and passes its body to a separate parser. */
public class ProductCatalogClient {
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http;

    /** Creates this component from the supplied values or dependencies. */
    public ProductCatalogClient() {
        this(URI.create("https://fakestoreapi.com/products"), REQUEST_TIMEOUT);
    }

    /** Creates this component from the supplied values or dependencies. */
    public ProductCatalogClient(URI endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** Downloads the catalog, checks its HTTP status and delegates JSON parsing. */
    public CatalogResponse fetch() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String info = Instant.now() + " GET " + endpoint + " -> HTTP " + response.statusCode();
        if (response.statusCode() != 200) {
            throw new IOException(info + ". Retry or enter manually.");
        }
        return new CatalogResponse(ProductCatalogParser.parse(response.body()), info);
    }
}
