package edu.uc.campusmarket.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import edu.uc.campusmarket.dto.CatalogProduct;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** Validates one complete catalog JSON array without making a network call. */
public final class ProductCatalogParser {
    private ProductCatalogParser() {
    }

    /** Validates and parses one complete catalog JSON array. */
    public static List<CatalogProduct> parse(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Expected one catalog JSON array.");
            }
            List<CatalogProduct> products = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                JsonObject item = element.getAsJsonObject();
                for (String key : List.of("id", "price")) {
                    if (!item.has(key) || !item.get(key).isJsonPrimitive()
                            || !item.get(key).getAsJsonPrimitive().isNumber()) {
                        throw new IOException("Catalog id and price must be numeric.");
                    }
                }
                for (String key : List.of("title", "category")) {
                    if (!item.has(key) || !item.get(key).isJsonPrimitive()
                            || !item.get(key).getAsJsonPrimitive().isString()) {
                        throw new IOException("Catalog title and category must be text.");
                    }
                }
                long id = item.get("id").getAsBigDecimal().longValueExact();
                String title = item.get("title").getAsString().trim();
                String category = item.get("category").getAsString().trim();
                var price = item.get("price").getAsBigDecimal();
                if (id <= 0 || title.isEmpty() || category.isEmpty() || price.signum() <= 0) {
                    throw new IOException("Catalog contains invalid product values.");
                }
                products.add(new CatalogProduct(id, title, category, price));
            }
            return List.copyOf(products);
        } catch (RuntimeException error) {
            throw new IOException("Invalid catalog JSON. Retry or enter manually.", error);
        }
    }
}
