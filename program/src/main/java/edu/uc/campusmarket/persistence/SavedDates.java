package edu.uc.campusmarket.persistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Reads legacy saved timestamps without inventing a date for damaged records.
 */
public final class SavedDates {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter
            .ofPattern("d MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private SavedDates() {
    }

    /** Returns null for absent or invalid saved timestamps, preserving the row. */
    public static Instant read(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException failure) {
            System.getLogger(SavedDates.class.getName()).log(System.Logger.Level.WARNING,
                    "A saved timestamp is invalid; showing its record without a date.");
            return null;
        }
    }

    /**
     * Formats a known saved time with its timezone, or explicitly reports it
     * unknown.
     */
    public static String display(String value) {
        Instant instant = read(value);
        return instant == null ? "Date unavailable" : DISPLAY.format(instant);
    }
}
