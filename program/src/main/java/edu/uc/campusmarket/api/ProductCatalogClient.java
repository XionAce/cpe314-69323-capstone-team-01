package edu.uc.campusmarket.api;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

/** Only this class knows how to request and parse the public product catalog. */
public class ProductCatalogClient {
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http;

    public ProductCatalogClient() {
        this(URI.create("https://fakestoreapi.com/products"), Duration.ofSeconds(15));
    }

    // A local HTTP server can be supplied by tests; the normal app uses the public URL.
    public ProductCatalogClient(URI endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public record Response(List<CatalogProduct> products, String requestInfo) { }

    public Response fetch() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String info = Instant.now() + " GET " + endpoint + " -> HTTP " + response.statusCode();
        System.out.println(info);
        if (response.statusCode() != 200) throw new IOException(info + ". Retry or enter manually.");
        return new Response(parse(response.body()), info);
    }

    public static List<CatalogProduct> parse(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("Expected one catalog JSON array.");
            List<CatalogProduct> products = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                JsonObject item = element.getAsJsonObject();
                long id = item.get("id").getAsBigDecimal().longValueExact();
                String title = item.get("title").getAsString().trim();
                String category = item.get("category").getAsString().trim();
                var price = item.get("price").getAsBigDecimal();
                if (id <= 0 || title.isEmpty() || category.isEmpty() || price.signum() <= 0)
                    throw new IOException("Catalog contains invalid product values.");
                products.add(new CatalogProduct(id, title, category, price));
            }
            return List.copyOf(products);
        } catch (RuntimeException error) {
            throw new IOException("Invalid catalog JSON. Retry or enter manually.", error);
        }
    }

    public static List<CatalogProduct> matches(List<CatalogProduct> products, String title, String type) {
        if (!"ELECTRONICS".equals(type)) return List.of();
        Set<String> words = new HashSet<>(Arrays.asList(title.toLowerCase(Locale.ROOT).split("\\W+")));
        words.removeIf(word -> word.length() < 3 || Set.of("the", "and", "for", "with").contains(word));
        return products.stream().filter(p -> p.category().equalsIgnoreCase("electronics"))
                .filter(p -> score(p, words) > 0)
                .sorted(Comparator.<CatalogProduct>comparingInt(p -> score(p, words)).reversed()
                        .thenComparingLong(CatalogProduct::id)).limit(3).toList();
    }

    private static int score(CatalogProduct product, Set<String> words) {
        Set<String> titleWords = new HashSet<>(Arrays.asList(product.title().toLowerCase(Locale.ROOT).split("\\W+")));
        titleWords.retainAll(words);
        return titleWords.size();
    }
}
