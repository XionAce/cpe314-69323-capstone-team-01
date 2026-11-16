package edu.uc.campusmarket.service;

import edu.uc.campusmarket.api.ProductCatalogClient;
import edu.uc.campusmarket.model.CatalogSuggestion;
import edu.uc.campusmarket.model.CatalogSuggestion.Status;
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
        return result(title, type).message();
    }

    /** Returns an explicit success, empty or failure outcome for the form. */
    public CatalogSuggestion result(String title, String type) {
        if (title == null || title.isBlank()) {
            return new CatalogSuggestion(Status.FAILED, "Enter a title first.");
        }
        try {
            var response = client.fetch();
            var matches = CatalogMapper.matches(response.products(), title, type);
            StringBuilder text = new StringBuilder(response.requestInfo() + "\n");
            if (matches.isEmpty()) {
                return new CatalogSuggestion(Status.EMPTY,
                        text + "No comparable product found. Enter category and price manually.");
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
            return new CatalogSuggestion(Status.OK, text + "Choose your final PHP price manually.");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return new CatalogSuggestion(Status.FAILED,
                    "Catalog request interrupted. Retry or enter manually.");
        } catch (IOException error) {
            System.getLogger(CatalogService.class.getName()).log(System.Logger.Level.WARNING,
                    "Catalog request failed.", error);
            return new CatalogSuggestion(Status.FAILED,
                    "Catalog unavailable. Retry or enter category and price manually.");
        }
    }
}
