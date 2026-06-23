package com.cl_labs;

public record Product(
    int id,
    String name,
    String category,
    int quantity,
    double price
) {}