package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListingTest {
    private Textbook book(int missingPages, WearLevel markings, boolean coverDamaged) {
        return new Textbook(0, 1, 2, "Java Book", new BigDecimal("350.50"),
                "PHP", "AVAILABLE", missingPages, markings, coverDamaged);
    }

    private Electronics laptop(boolean power, boolean defect, boolean scratches) {
        return new Electronics(0, 1, 7, "Laptop", new BigDecimal("8000"),
                "PHP", "AVAILABLE", power, defect, scratches);
    }

    private Furniture chair(boolean stable, boolean damage, WearLevel wear) {
        return new Furniture(0, 1, 4, "Chair", new BigDecimal("500"),
                "PHP", "AVAILABLE", stable, damage, wear);
    }

    @Test
    void sharedListingReferenceUsesEachSubclassRule() {
        Listing[] items = {book(0, WearLevel.NONE, false),
                laptop(true, false, true), chair(true, false, WearLevel.HEAVY)};
        assertEquals(ConditionGrade.LIKE_NEW, items[0].gradeCondition());
        assertEquals(ConditionGrade.GOOD, items[1].gradeCondition());
        assertEquals(ConditionGrade.FAIR, items[2].gradeCondition());
    }

    @Test
    void missingPageWinsEvenWithHeavyMarkingsAndCoverDamage() {
        assertEquals(ConditionGrade.POOR, book(1, WearLevel.HEAVY, true).gradeCondition());
    }

    @Test
    void textbookMarkingsAndCoverHaveTheirOwnBoundaries() {
        assertEquals(ConditionGrade.FAIR, book(0, WearLevel.HEAVY, true).gradeCondition());
        assertEquals(ConditionGrade.GOOD, book(0, WearLevel.LIGHT, false).gradeCondition());
        assertEquals(ConditionGrade.GOOD, book(0, WearLevel.NONE, true).gradeCondition());
        assertThrows(IllegalArgumentException.class, () -> book(-1, WearLevel.NONE, false));
        assertThrows(IllegalArgumentException.class, () -> book(0, null, false));
    }

    @Test
    void electronicsFunctionalFailureWinsOverAppearance() {
        assertEquals(ConditionGrade.POOR, laptop(false, false, false).gradeCondition());
        assertEquals(ConditionGrade.POOR, laptop(true, true, true).gradeCondition());
        assertEquals(ConditionGrade.LIKE_NEW, laptop(true, false, false).gradeCondition());
    }

    @Test
    void unstableOrDamagedFurnitureCannotReceiveCosmeticGrade() {
        assertEquals(ConditionGrade.POOR, chair(false, false, WearLevel.NONE).gradeCondition());
        assertEquals(ConditionGrade.POOR, chair(true, true, WearLevel.HEAVY).gradeCondition());
        assertEquals(ConditionGrade.GOOD, chair(true, false, WearLevel.LIGHT).gradeCondition());
        assertEquals(ConditionGrade.LIKE_NEW, chair(true, false, WearLevel.NONE).gradeCondition());
        assertThrows(IllegalArgumentException.class, () -> chair(true, false, null));
    }

    @Test
    void moneyUsesExactCentsWithoutFloatingPointRounding() {
        assertEquals(35050, book(0, WearLevel.NONE, false).getPriceMinor());
        assertEquals(new BigDecimal("350.50"), book(0, WearLevel.NONE, false).getPrice());
        Textbook trailingZeros = new Textbook(0, 1, 2, "Book", new BigDecimal("1.2300"),
                "PHP", "AVAILABLE", 0, WearLevel.NONE, false);
        assertEquals(123, trailingZeros.getPriceMinor());
    }

    @Test
    void invalidMoneyIsRejectedBeforeItCanReachSqlite() {
        String[] invalidPrices = {"0", "-1", "1.001", "92233720368547758.08"};
        for (String price : invalidPrices) {
            assertThrows(IllegalArgumentException.class, () -> new Textbook(0, 1, 2, "Book",
                    new BigDecimal(price), "PHP", "AVAILABLE", 0, WearLevel.NONE, false));
        }
    }

    @Test
    void commonFieldsRejectBadIdentityCurrencyAndStatus() {
        assertThrows(IllegalArgumentException.class, () -> new Textbook(-1, 1, 2, "Book",
                BigDecimal.ONE, "PHP", "AVAILABLE", 0, WearLevel.NONE, false));
        assertThrows(IllegalArgumentException.class, () -> new Textbook(0, 0, 2, "Book",
                BigDecimal.ONE, "PHP", "AVAILABLE", 0, WearLevel.NONE, false));
        assertThrows(IllegalArgumentException.class, () -> new Textbook(0, 1, 2, " ",
                BigDecimal.ONE, "PHP", "AVAILABLE", 0, WearLevel.NONE, false));
        assertThrows(IllegalArgumentException.class, () -> new Textbook(0, 1, 2, "Book",
                BigDecimal.ONE, "php", "AVAILABLE", 0, WearLevel.NONE, false));
        assertThrows(IllegalArgumentException.class, () -> new Textbook(0, 1, 2, "Book",
                BigDecimal.ONE, "PHP", "UNKNOWN", 0, WearLevel.NONE, false));
    }
}
