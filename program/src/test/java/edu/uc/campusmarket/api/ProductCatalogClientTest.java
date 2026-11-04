package edu.uc.campusmarket.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogClientTest {
    private static final String JSON = """
            [{"id":1,"title":"Portable gaming monitor","category":"electronics","price":120.50},
             {"id":2,"title":"Gaming shirt","category":"men's clothing","price":10}]
            """;

    @Test void parsesAndMatchesOnlyCompatibleProducts() throws Exception {
        var products = ProductCatalogClient.parse(JSON);
        var matches = ProductCatalogClient.matches(products, "gaming monitor", "ELECTRONICS");
        assertEquals(1, matches.size());
        assertEquals("120.50", matches.getFirst().price().toPlainString());
        assertTrue(ProductCatalogClient.matches(products, "monitor", "TEXTBOOK").isEmpty());
        assertTrue(ProductCatalogClient.matches(products, "unrelated", "ELECTRONICS").isEmpty());
    }

    @Test void emptyAndInvalidResponsesHaveDefinedResults() throws Exception {
        assertTrue(ProductCatalogClient.parse("[]").isEmpty());
        for (String bad : new String[]{"{}", "[", "[] trailing", "[{\"id\":1}]",
                "[{\"id\":1,\"title\":\"x\",\"category\":\"electronics\",\"price\":-1}]"})
            assertThrows(IOException.class, () -> ProductCatalogClient.parse(bad), bad);
    }

    @Test void realHttpRequestParsesSuccessAndRejectsHttpFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/products", exchange -> {
            byte[] body = JSON.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/error", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            var response = new ProductCatalogClient(URI.create(base + "/products"), Duration.ofSeconds(3)).fetch();
            assertEquals(2, response.products().size());
            assertTrue(response.requestInfo().contains("HTTP 200"));
            assertThrows(IOException.class, () -> new ProductCatalogClient(URI.create(base + "/error"), Duration.ofSeconds(3)).fetch());
        } finally { server.stop(0); }
    }
}
