package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dto.CatalogProduct;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps supported catalog categories to comparable campus listing suggestions.
 */
public final class CatalogMapper {
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_SUGGESTIONS = 3;

    private CatalogMapper() {
    }

    /** Selects compatible electronics references and ranks their title matches. */
    public static List<CatalogProduct> matches(List<CatalogProduct> products, String title,
            String type) {
        if (!"ELECTRONICS".equals(type)) {
            return List.of();
        }
        Set<String> words = new HashSet<>(
                Arrays.asList(title.toLowerCase(Locale.ROOT).split("\\W+")));
        words.removeIf(word -> word.length() < MIN_WORD_LENGTH
                || Set.of("the", "and", "for", "with").contains(word));
        return products.stream().filter(p -> p.category().equalsIgnoreCase("electronics"))
                .filter(p -> score(p, words) > 0)
                .sorted(Comparator.<CatalogProduct>comparingInt(p -> score(p, words)).reversed()
                        .thenComparingLong(CatalogProduct::id))
                .limit(MAX_SUGGESTIONS).toList();
    }

    private static int score(CatalogProduct product, Set<String> words) {
        Set<String> titleWords = new HashSet<>(
                Arrays.asList(product.title().toLowerCase(Locale.ROOT).split("\\W+")));
        titleWords.retainAll(words);
        return titleWords.size();
    }
}
