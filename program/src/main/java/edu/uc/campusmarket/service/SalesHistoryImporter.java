package edu.uc.campusmarket.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.HistoryDao;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.ImportResult;
import edu.uc.campusmarket.persistence.Database;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Reads one sale at a time. A transaction makes cancellation/failure undo the
 * new import.
 */
public class SalesHistoryImporter {
    private static final int MAX_VALIDATION_MESSAGES = 5;
    private static final int MONEY_SCALE = 2;
    private final Database database;
    private final CategoryDao categoriesDao;
    private final HistoryDao historyDao;

    /** Creates this component from the supplied values or dependencies. */
    public SalesHistoryImporter(Database database, CategoryDao categoriesDao,
            HistoryDao historyDao) {
        this.database = database;
        this.categoriesDao = categoriesDao;
        this.historyDao = historyDao;
    }

    /**
     * Reads historical-sales JSON with progress, cancellation and transactional
     * writes.
     */
    public ImportResult importFile(Path file, BooleanSupplier cancelled, LongConsumer progress)
            throws IOException, SQLException {
        List<Category> categories = categoriesDao.getCategories();
        try (var connection = database.open();
                var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            connection.setAutoCommit(false);
            reader.setStrictness(Strictness.STRICT);
            try (var insert = historyDao.prepareImport(connection)) {
                reader.beginArray();
                ImportResult result = readRows(reader, insert, categories, cancelled, progress);
                reader.endArray();
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    throw new IOException("Unexpected data after JSON array.");
                }
                checkCancelled(cancelled);
                connection.commit();
                return result;
            } catch (IOException | SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                if (error instanceof CancellationException) {
                    throw error;
                }
                throw new IOException("Import failed; no new sales were saved. " + error.getMessage(), error);
            }
        }
    }

    private ImportResult readRows(JsonReader reader, java.sql.PreparedStatement insert,
            List<Category> categories, BooleanSupplier cancelled, LongConsumer progress)
            throws IOException, SQLException {
        int saved = 0, invalid = 0, duplicates = 0;
        List<String> errors = new ArrayList<>();
        while (reader.hasNext()) {
            checkCancelled(cancelled);
            // Broken JSON escapes field validation and rolls back the whole import.
            JsonElement element = JsonParser.parseReader(reader);
            SaleRow row;
            try {
                row = validateRow(element.getAsJsonObject(), categories);
            } catch (RuntimeException error) {
                invalid++;
                if (errors.size() < MAX_VALIDATION_MESSAGES) {
                    errors.add("Record " + (saved + invalid + duplicates)
                            + ": invalid fields, price, date or category.");
                }
                progress.accept(saved + invalid + duplicates);
                continue;
            }
            bindSale(insert, row);
            if (insert.executeUpdate() == 1) {
                saved++;
            } else {
                duplicates++;
            }
            progress.accept(saved + invalid + duplicates);
        }
        return new ImportResult(saved, invalid, duplicates, List.copyOf(errors));
    }

    private record SaleRow(long categoryId, String sourceId, long minor, String currency, String soldAt) { }

    private SaleRow validateRow(JsonObject row, List<Category> categories) {
        String sourceId = text(row, "sourceRecordId");
        String currency = text(row, "currency");
        String soldAt = text(row, "soldAt");
        OffsetDateTime.parse(soldAt);
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Invalid currency.");
        }
        if (!row.get("price").getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Price must be numeric.");
        }
        long minor = row.get("price").getAsBigDecimal().movePointRight(MONEY_SCALE).longValueExact();
        if (minor <= 0) {
            throw new IllegalArgumentException("Price must be positive with at most two decimals.");
        }
        return new SaleRow(categoryId(row.getAsJsonArray("categoryPath"), categories),
                sourceId, minor, currency, soldAt);
    }

    private void bindSale(java.sql.PreparedStatement insert, SaleRow row) throws SQLException {
        insert.setLong(1, row.categoryId());
        insert.setString(2, row.sourceId());
        insert.setLong(3, row.minor());
        insert.setString(4, row.currency());
        insert.setString(5, row.soldAt());
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Import cancelled; no new sales were saved.");
        }
    }

    private static String text(JsonObject row, String key) {
        if (!row.has(key) || !row.get(key).isJsonPrimitive()
                || !row.get(key).getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing text: " + key);
        }
        String value = row.get(key).getAsString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Blank " + key);
        }
        return value;
    }

    private static long categoryId(JsonArray path, List<Category> categories) {
        Long parent = null;
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Missing category path.");
        }
        for (JsonElement part : path) {
            String name = part.getAsString();
            List<Category> matches = new ArrayList<>();
            for (Category category : categories) {
                if (Objects.equals(category.getParentId(), parent)
                        && category.getName().equals(name)) {
                    matches.add(category);
                }
            }
            if (matches.size() != 1) {
                throw new IllegalArgumentException("Unknown/ambiguous category path.");
            }
            parent = matches.getFirst().getId();
        }
        return parent;
    }
}
