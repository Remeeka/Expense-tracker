package com.example.expense.service;

import com.example.expense.dto.CategorySummary;
import com.example.expense.dto.ExpenseRequest;
import com.example.expense.dto.ExpenseResponse;
import com.example.expense.dto.MonthlySummaryResponse;
import com.example.expense.exception.ExpenseNotFoundException;
import com.example.expense.model.Expense;
import com.example.expense.repository.ExpenseRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    public ExpenseResponse getExpense(String id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Expense expense = new Expense();
        applyRequest(request, expense);
        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    public ExpenseResponse updateExpense(String id, ExpenseRequest request) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        applyRequest(request, existing);
        Expense saved = expenseRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteExpense(String id) {
        if (!expenseRepository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }
        expenseRepository.deleteById(id);
    }

    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        YearMonth yearMonth = validateYearMonth(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByDateBetween(start, end);

        Map<com.example.expense.model.Category, BigDecimal> totals = new LinkedHashMap<>();
        expenses.forEach(expense -> totals.merge(
                expense.getCategory(),
                expense.getAmount(),
                BigDecimal::add));

        List<CategorySummary> summaries = totals.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CategorySummary(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new MonthlySummaryResponse(yearMonth.getYear(), yearMonth.getMonthValue(), totalAmount, summaries);
    }

    public byte[] exportMonthlyExpensesToCsv(int year, int month) {
        YearMonth yearMonth = validateYearMonth(year, month);
        List<Expense> expenses = findByYearMonth(yearMonth);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Id", "Title", "Category", "Amount", "Date", "Notes"))) {

            for (Expense expense : expenses) {
                printer.printRecord(
                        expense.getId(),
                        expense.getTitle(),
                        expense.getCategory().name(),
                        expense.getAmount(),
                        expense.getDate(),
                        expense.getNotes() != null ? expense.getNotes() : "");
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export CSV", ex);
        }
    }

    public byte[] exportMonthlyExpensesToPdf(int year, int month) {
        YearMonth yearMonth = validateYearMonth(year, month);
        List<Expense> expenses = findByYearMonth(yearMonth)
                .stream()
                .sorted(Comparator.comparing(Expense::getDate))
                .toList();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Expense Report - " + yearMonth);
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float yPosition = 690;
                for (Expense expense : expenses) {
                    if (yPosition < 100) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        yPosition = 700;
                    }
                    try (PDPageContentStream lineStream = new PDPageContentStream(document, page,
                            PDPageContentStream.AppendMode.APPEND, true)) {
                        lineStream.beginText();
                        lineStream.newLineAtOffset(50, yPosition);
                        lineStream.showText(formatExpenseLine(expense));
                        lineStream.endText();
                    }
                    yPosition -= 18;
                }
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export PDF", ex);
        }
    }

    private List<Expense> findByYearMonth(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return expenseRepository.findByDateBetween(start, end);
    }

    private void applyRequest(ExpenseRequest request, Expense expense) {
        expense.setTitle(request.getTitle());
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        expense.setDate(request.getDate());
        expense.setNotes(request.getNotes());
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getDate(),
                expense.getNotes()
        );
    }

    private YearMonth validateYearMonth(int year, int month) {
        if (year < 2000 || year > 3000) {
            throw new IllegalArgumentException("Year must be between 2000 and 3000");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        return YearMonth.of(year, month);
    }

    private String formatExpenseLine(Expense expense) {
        return String.format("%s | %s | %s | %s | %s",
                expense.getDate(),
                expense.getTitle(),
                expense.getCategory().name(),
                expense.getAmount(),
                expense.getNotes() == null ? "" : expense.getNotes());
    }
}

