package com.example.expense.frontend.model;

import java.math.BigDecimal;
import java.util.List;

public class MonthlySummary {

    private int year;
    private int month;
    private BigDecimal totalAmount;
    private List<CategorySummary> categories;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<CategorySummary> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySummary> categories) {
        this.categories = categories;
    }
}

