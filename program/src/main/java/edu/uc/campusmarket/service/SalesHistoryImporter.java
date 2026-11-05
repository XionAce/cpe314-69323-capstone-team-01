package edu.uc.campusmarket.service;

import com.google.gson.*;
import com.google.gson.stream.*;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.persistence.Database;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;

/** Reads one sale at a time. A transaction makes cancellation/failure undo the new import. */
public class SalesHistoryImporter {
    private final Database database;
    public SalesHistoryImporter(Database database) { this.database = database; }
    public record Result(int saved, int invalid, int duplicates, List<String> errors) { }

    public Result importFile(Path file, BooleanSupplier cancelled, LongConsumer progress) throws IOException, SQLException {
        List<Category> categories = database.getCategories();
        int saved = 0, invalid = 0, duplicates = 0;
        List<String> errors = new ArrayList<>();
        try (var connection = database.open();
             var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            connection.setAutoCommit(false);
            reader.setStrictness(Strictness.STRICT);
            try (var insert = connection.prepareStatement("""
                    INSERT INTO HistoricalSales(categoryId, sourceRecordId, priceMinor, currency, soldAt)
                    VALUES (?, ?, ?, ?, ?) ON CONFLICT(sourceRecordId) DO NOTHING
                    """)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    checkCancelled(cancelled);
                    // Syntax errors must escape the row-validation catch and roll back the file.
                    JsonElement element = JsonParser.parseReader(reader);
                    long categoryId, minor;
                    String sourceId, currency, soldAt;
                    try {
                        JsonObject row = element.getAsJsonObject();
                        sourceId = text(row, "sourceRecordId");
                        currency = text(row, "currency");
                        soldAt = text(row, "soldAt");
                        OffsetDateTime.parse(soldAt);
                        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid currency.");
                        if (!row.get("price").getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Price must be numeric.");
                        minor = row.get("price").getAsBigDecimal().movePointRight(2).longValueExact();
                        if (minor <= 0) throw new IllegalArgumentException("Price must be positive with at most two decimals.");
                        categoryId = categoryId(row.getAsJsonArray("categoryPath"), categories);
                    } catch (RuntimeException error) {
                        invalid++;
                        if (errors.size() < 5) errors.add("Record " + (saved + invalid + duplicates) + ": invalid fields, price, date or category.");
                        progress.accept(saved + invalid + duplicates);
                        continue;
                    }
                    insert.setLong(1, categoryId); insert.setString(2, sourceId);
                    insert.setLong(3, minor); insert.setString(4, currency); insert.setString(5, soldAt);
                    if (insert.executeUpdate() == 1) saved++; else duplicates++;
                    progress.accept(saved + invalid + duplicates);
                }
                reader.endArray();
                if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Unexpected data after JSON array.");
                checkCancelled(cancelled);
                connection.commit();
                return new Result(saved, invalid, duplicates, List.copyOf(errors));
            } catch (IOException | SQLException | RuntimeException error) {
                try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                if (error instanceof CancellationException) throw error;
                throw new IOException("Import failed; no new sales were saved. " + error.getMessage(), error);
            }
        }
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new CancellationException("Import cancelled; no new sales were saved.");
    }

    private static String text(JsonObject row, String key) {
        if (!row.has(key) || !row.get(key).isJsonPrimitive() || !row.get(key).getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Missing text: " + key);
        String value = row.get(key).getAsString().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Blank " + key);
        return value;
    }

    private static long categoryId(JsonArray path, List<Category> categories) {
        Long parent = null;
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("Missing category path.");
        for (JsonElement part : path) {
            String name = part.getAsString();
            List<Category> matches = new ArrayList<>();
            for (Category category : categories)
                if (Objects.equals(category.getParentId(), parent) && category.getName().equals(name)) matches.add(category);
            if (matches.size() != 1) throw new IllegalArgumentException("Unknown/ambiguous category path.");
            parent = matches.getFirst().getId();
        }
        return parent;
    }
}
