package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListingValidationTest {

    @Test
    void categoryRejectsInvalidParentsAndKeepsRootParentEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new Category(0, null, "Root"));
        assertThrows(IllegalArgumentException.class, () -> new Category(1, 1L, "Self parent"));
        assertThrows(IllegalArgumentException.class, () -> new Category(1, 0L, "Invalid parent"));
        assertThrows(IllegalArgumentException.class, () -> new Category(1, null, " "));
        Category root = new Category(1, null, "  All finds  ");
        assertEquals(null, root.getParentId());
        assertEquals("All finds", root.getName());
    }

    @Test
    void memberRejectsMissingIdentityAndTrimsAcceptedNames() {
        assertThrows(IllegalArgumentException.class, () -> new Member(0, "Person"));
        assertThrows(IllegalArgumentException.class, () -> new Member(1, null));
        assertThrows(IllegalArgumentException.class, () -> new Member(1, " "));
        assertEquals("Person A", new Member(1, "  Person A  ").getName());
    }

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
