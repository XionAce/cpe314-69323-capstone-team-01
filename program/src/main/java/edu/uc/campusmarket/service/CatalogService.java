package edu.uc.campusmarket.service;

import edu.uc.campusmarket.api.ProductCatalogClient;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Presents supported DTO matches as references without inventing PHP
 * conversions.
 */
public class CatalogService {
    private final ProductCatalogClient client;

    /** Creates this component from the supplied values or dependencies. */
    public CatalogService(ProductCatalogClient client) {
        this.client = client;
    }

    /**
     * Requests supported catalog references without treating their prices as PHP.
     */
    public String suggest(String title, String type) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Enter a title first.");
        }
        try {
            var response = client.fetch();
            var matches = CatalogMapper.matches(response.products(), title, type);
            StringBuilder text = new StringBuilder(response.requestInfo() + "\n");
            if (matches.isEmpty()) {
                return text + "No comparable product found. Enter category and price manually.";
            }
            BigDecimal min = matches.stream().map(p -> p.price()).min(BigDecimal::compareTo)
                    .orElseThrow();
            BigDecimal max = matches.stream().map(p -> p.price()).max(BigDecimal::compareTo)
                    .orElseThrow();
            text.append("Electronics catalog reference: ").append(min).append(" - ").append(max)
                    .append(" (currency unspecified).\n");
            for (var product : matches) {
                text.append(product.title()).append("\n");
            }
            return text + "Choose your final PHP price manually.";
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AppException("Catalog request interrupted. Retry or enter manually.", error);
        } catch (IOException error) {
            throw new AppException(error.getMessage(), error);
        }
    }
}
