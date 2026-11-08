package edu.uc.campusmarket.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import edu.uc.campusmarket.api.CatalogProduct;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ProductCatalogClientTest {
    private static final String JSON = """
            [{"id":1,"title":"Portable gaming monitor","category":"electronics","price":120.50},
             {"id":2,"title":"Gaming shirt","category":"men's clothing","price":10}]
            """;

    @Test
    void suggestionsRankSharedWordsThenProductIdAndReturnOnlyThree() {
        List<CatalogProduct> products = List.of(
                new CatalogProduct(5, "Gaming monitor", "electronics", BigDecimal.TEN),
                new CatalogProduct(4, "Gaming monitor", "electronics", BigDecimal.TEN),
                new CatalogProduct(3, "Monitor", "electronics", BigDecimal.TEN),
                new CatalogProduct(2, "Portable gaming monitor", "electronics", BigDecimal.TEN),
                new CatalogProduct(1, "Portable gaming monitor", "furniture", BigDecimal.TEN));
        var matches = ProductCatalogClient.matches(products, "Portable gaming monitor", "ELECTRONICS");
        assertEquals(List.of(2L, 4L, 5L), matches.stream().map(CatalogProduct::id).toList());
    }

    @Test
    void suggestionsIgnorePunctuationCaseShortWordsAndStopWords() {
        var monitor = new CatalogProduct(1, "MONITOR", "Electronics", BigDecimal.TEN);
        var stopWords = new CatalogProduct(2, "the and for with", "electronics", BigDecimal.TEN);
        var products = List.of(monitor, stopWords);
        assertEquals(List.of(monitor), ProductCatalogClient.matches(products, "The monitor!!! for me", "ELECTRONICS"));
        assertTrue(ProductCatalogClient.matches(products, "the and for with TV", "ELECTRONICS").isEmpty());
        assertTrue(ProductCatalogClient.matches(products, "monitor", "FURNITURE").isEmpty());
    }

    @Test
    void catalogRejectsInvalidIdentityBlankFieldsAndNullProducts() {
        String valid = "{\"id\":1,\"title\":\"Monitor\",\"category\":\"electronics\",\"price\":10}";
        String[] invalid = {valid.replace("\"id\":1", "\"id\":0"),
                valid.replace("\"id\":1", "\"id\":1.5"), valid.replace("Monitor", "   "),
                valid.replace("electronics", ""), valid.replace("\"price\":10", "\"price\":0"), "null"};
        for (String item : invalid) {
            assertThrows(IOException.class, () -> ProductCatalogClient.parse("[" + item + "]"), item);
        }
    }

    @Test
    void catalogPreservesExactDecimalPricesAndIgnoresUnusedResponseFields() throws Exception {
        var products = ProductCatalogClient.parse("""
                [{"id":7,"title":"  Monitor  ","category":" electronics ","price":123.45,
                  "description":"Extra data","rating":{"rate":4.5,"count":10},"image":"example.png"}]
                """);
        assertEquals(new BigDecimal("123.45"), products.getFirst().price());
        assertEquals("Monitor", products.getFirst().title());
        assertEquals("electronics", products.getFirst().category());
    }

    @Test
    void httpSuccessWithMalformedJsonStillFailsParsing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/invalid", exchange -> {
            byte[] body = "<html>Unavailable</html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            URI endpoint = URI.create("http://localhost:" + server.getAddress().getPort() + "/invalid");
            assertThrows(IOException.class, () -> new ProductCatalogClient(endpoint, Duration.ofSeconds(3)).fetch());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestTimeoutStopsWaitingForAnUnresponsiveServer() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/slow", exchange -> {
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            URI endpoint = URI.create("http://localhost:" + server.getAddress().getPort() + "/slow");
            assertTimeoutPreemptively(Duration.ofSeconds(4), () -> assertThrows(
                    java.net.http.HttpTimeoutException.class,
                    () -> new ProductCatalogClient(endpoint, Duration.ofMillis(200)).fetch()));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

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
