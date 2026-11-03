package edu.uc.campusmarket.api;

import java.math.BigDecimal;

/** A record is a small data holder; Java supplies its constructor and accessors. */
public record CatalogProduct(long id, String title, String category, BigDecimal price) { }
