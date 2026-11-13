package edu.uc.campusmarket.dto;

import java.util.List;

/** Products and HTTP information shown by the suggestion feature. */
public record CatalogResponse(List<CatalogProduct> products, String requestInfo) {
}
