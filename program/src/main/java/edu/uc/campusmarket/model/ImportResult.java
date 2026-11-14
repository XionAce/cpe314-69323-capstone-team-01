package edu.uc.campusmarket.model;

import java.util.List;

/** Immutable data passed between the application layers. */
public record ImportResult(int saved, int invalid, int duplicates, List<String> errors) {
}
