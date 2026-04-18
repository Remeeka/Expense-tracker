package com.example.expense.repository;

import com.example.expense.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByDateBetween(LocalDate start, LocalDate end);
}

