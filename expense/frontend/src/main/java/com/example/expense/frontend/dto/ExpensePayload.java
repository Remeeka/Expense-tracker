package com.example.expense.frontend.dto;

import com.example.expense.frontend.model.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpensePayload {

    private String title;
    private Category category;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;

    public ExpensePayload() {
    }

    public ExpensePayload(String title,
                          Category category,
                          BigDecimal amount,
                          LocalDate date,
                          String notes) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

