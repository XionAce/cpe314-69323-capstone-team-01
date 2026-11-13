package edu.uc.campusmarket.model;

import edu.uc.campusmarket.service.ListingFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListingValidationTest {

    @Test
    void factoryRejectsMissingOrUnsupportedTypeInsteadOfCreatingFurniture() {
        for (String type : new String[] { null, "", "ARTWORK" }) {
            assertThrows(IllegalArgumentException.class, () -> ListingFactory.create(
                    0, 1, 2, "Item", BigDecimal.TEN, "PHP", Listing.ACTIVE,
                    type, "No visible damage"));
        }
    }

    @Test
    void factoryRejectsMissingOrMismatchedEvidenceInsteadOfInventingAGoodCondition() {
        for (String evidence : new String[] { null, "", "Light wear", "Unknown condition" }) {
            assertThrows(IllegalArgumentException.class, () -> ListingFactory.create(
                    0, 1, 2, "Book", BigDecimal.TEN, "PHP", Listing.ACTIVE,
                    "TEXTBOOK", evidence));
        }
        Listing valid = ListingFactory.create(0, 1, 2, "Book", BigDecimal.TEN,
                "PHP", Listing.ACTIVE, "TEXTBOOK", "Light markings");
        assertEquals(ConditionGrade.GOOD, valid.gradeCondition());
    }

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
        return new Textbook(0, 1, 2, title, price, "PHP", "ACTIVE", 0, WearLevel.NONE, false);
    }

    @Test
    void removesSpacesAroundTitle() {
        Textbook book = textbook("  Java textbook  ", new BigDecimal("250.00"));

        assertEquals("Java textbook", book.getTitle());
    }

    @Test
    void acceptsMaximumPriceThatFitsInStorage() {
        Textbook book = textbook("Java textbook", new BigDecimal("92233720368547758.07"));

        assertEquals(Long.MAX_VALUE, book.getPriceMinor());
    }
}
