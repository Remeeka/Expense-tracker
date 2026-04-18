package com.example.expense.dto;

import com.example.expense.model.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseResponse {

    private String id;
    private String title;
    private Category category;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;

    public ExpenseResponse(String id,
                           String title,
                           Category category,
                           BigDecimal amount,
                           LocalDate date,
                           String notes) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }
}

