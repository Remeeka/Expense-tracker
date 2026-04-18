package com.example.expense.frontend.model;

import java.math.BigDecimal;

public class CategorySummary {

    private Category category;
    private BigDecimal totalAmount;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}

