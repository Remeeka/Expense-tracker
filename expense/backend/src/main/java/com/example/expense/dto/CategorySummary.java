package com.example.expense.dto;

import com.example.expense.model.Category;

import java.math.BigDecimal;

public class CategorySummary {

    private final Category category;
    private final BigDecimal totalAmount;

    public CategorySummary(Category category, BigDecimal totalAmount) {
        this.category = category;
        this.totalAmount = totalAmount;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}

