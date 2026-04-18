package com.example.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlySummaryResponse {

    private final int year;
    private final int month;
    private final BigDecimal totalAmount;
    private final List<CategorySummary> categories;

    public MonthlySummaryResponse(int year,
                                  int month,
                                  BigDecimal totalAmount,
                                  List<CategorySummary> categories) {
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
        this.categories = categories;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<CategorySummary> getCategories() {
        return categories;
    }
}

