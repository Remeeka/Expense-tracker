package com.example.expense.frontend.viewmodel;

import com.example.expense.frontend.model.Expense;
import com.example.expense.frontend.model.MonthlySummary;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class ExpenseViewModel {

    private final ObservableList<Expense> expenses = FXCollections.observableArrayList();
    private final ObjectProperty<Expense> selectedExpense = new SimpleObjectProperty<>();
    private final ObjectProperty<MonthlySummary> monthlySummary = new SimpleObjectProperty<>();

    public ObservableList<Expense> getExpenses() {
        return expenses;
    }

    public ObjectProperty<Expense> selectedExpenseProperty() {
        return selectedExpense;
    }

    public Expense getSelectedExpense() {
        return selectedExpense.get();
    }

    public void setSelectedExpense(Expense expense) {
        selectedExpense.set(expense);
    }

    public ObjectProperty<MonthlySummary> monthlySummaryProperty() {
        return monthlySummary;
    }

    public MonthlySummary getMonthlySummary() {
        return monthlySummary.get();
    }

    public void setMonthlySummary(MonthlySummary summary) {
        monthlySummary.set(summary);
    }

    public void replaceExpenses(Iterable<Expense> newExpenses) {
        List<Expense> expenseList = new ArrayList<>();
        newExpenses.forEach(expenseList::add); // convert Iterable to List
        expenses.setAll(expenseList);
    }
}
