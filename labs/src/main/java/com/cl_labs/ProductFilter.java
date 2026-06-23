package com.cl_labs;

public record ProductFilter(
    String name,
    String category,
    Integer minQty,
    Integer maxQty,
    Double minPrice,
    Double maxPrice,
    int limit,
    int offset
) {}