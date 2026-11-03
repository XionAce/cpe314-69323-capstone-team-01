package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingValidationTest {

    private Textbook textbook(String title, BigDecimal price) {
        return new Textbook(
                0, 1, 2, title, price,
                "PHP", "AVAILABLE",
                0, WearLevel.NONE, false);
    }

    @Test
    void removesSpacesAroundTitle() {
        Textbook book = textbook(
                "  Java textbook  ",
                new BigDecimal("250.00"));

        assertEquals("Java textbook", book.getTitle());
    }

    @Test
    void acceptsMaximumPriceThatFitsInStorage() {
        Textbook book = textbook(
                "Java textbook",
                new BigDecimal("92233720368547758.07"));

        assertEquals(Long.MAX_VALUE, book.getPriceMinor());
    }
}