package com.example.expense.frontend.ui;

import com.example.expense.frontend.api.ExpenseApiClient;
import com.example.expense.frontend.dto.ExpensePayload;
import com.example.expense.frontend.model.Category;
import com.example.expense.frontend.model.CategorySummary;
import com.example.expense.frontend.model.Expense;
import com.example.expense.frontend.model.MonthlySummary;
import com.example.expense.frontend.viewmodel.ExpenseViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart; // ✅ Added missing import
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class MainView extends BorderPane {

    private final ExpenseApiClient apiClient;
    private final ExpenseViewModel viewModel;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final TableView<Expense> expenseTable = new TableView<>();
    private final TextField titleField = new TextField();
    private final ComboBox<Category> categoryCombo = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextArea notesArea = new TextArea();

    private final ComboBox<Integer> monthSelector = new ComboBox<>();
    private final ComboBox<Integer> yearSelector = new ComboBox<>();
    private final PieChart summaryChart = new PieChart();

    public MainView(ExpenseApiClient apiClient, ExpenseViewModel viewModel) {
        this.apiClient = apiClient;
        this.viewModel = viewModel;
        setPadding(new Insets(16));
        buildLayout();
        bindViewModel();
        loadInitialData();
    }

    private void buildLayout() {
        setTop(buildForm());
        setCenter(buildCenter());
        setRight(buildSummaryPanel());
    }

    private Node buildForm() {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(0, 0, 16, 0));

        titleField.setPromptText("Description");
        amountField.setPromptText("Amount");
        notesArea.setPromptText("Notes");
        notesArea.setPrefRowCount(2);

        categoryCombo.setItems(FXCollections.observableArrayList(Category.values()));
        categoryCombo.getSelectionModel().select(Category.OTHER);

        form.add(new Label("Title"), 0, 0);
        form.add(titleField, 1, 0);
        form.add(new Label("Category"), 2, 0);
        form.add(categoryCombo, 3, 0);

        form.add(new Label("Amount"), 0, 1);
        form.add(amountField, 1, 1);
        form.add(new Label("Date"), 2, 1);
        form.add(datePicker, 3, 1);

        form.add(new Label("Notes"), 0, 2);
        form.add(notesArea, 1, 2, 3, 1);

        HBox buttons = new HBox(8);
        Button saveButton = new Button("Save");
        Button resetButton = new Button("Reset");
        Button deleteButton = new Button("Delete");

        saveButton.setOnAction(event -> onSave());
        resetButton.setOnAction(event -> clearForm());
        deleteButton.setOnAction(event -> onDelete());
        buttons.getChildren().addAll(saveButton, resetButton, deleteButton);
        form.add(buttons, 1, 3, 3, 1);

        return form;
    }

    private Node buildCenter() {
        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);

        TableColumn<Expense, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Expense, Category> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Expense, BigDecimal> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Expense, LocalDate> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        expenseTable.getColumns().addAll(titleColumn, categoryColumn, amountColumn, dateColumn);
        expenseTable.setItems(viewModel.getExpenses());
        expenseTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> viewModel.setSelectedExpense(newSel));

        VBox tableContainer = new VBox(expenseTable);
        VBox.setVgrow(expenseTable, Priority.ALWAYS);

        return tableContainer;
    }

    private Node buildSummaryPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(0, 0, 0, 16));
        panel.setPrefWidth(320);

        YearMonth currentMonth = YearMonth.now();

        monthSelector.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(1, 12).boxed().toList()
        ));
        monthSelector.getSelectionModel().select(Integer.valueOf(currentMonth.getMonthValue()));
        monthSelector.valueProperty().addListener((obs, oldVal, newVal) -> loadSummary());

        int currentYear = currentMonth.getYear();
        yearSelector.setItems(FXCollections.observableArrayList(
                currentYear - 1, currentYear, currentYear + 1
        ));
        yearSelector.getSelectionModel().select(Integer.valueOf(currentYear));
        yearSelector.valueProperty().addListener((obs, oldVal, newVal) -> loadSummary());

        HBox selectors = new HBox(8, new Label("Month"), monthSelector, new Label("Year"), yearSelector);

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> {
            loadExpenses();
            loadSummary();
        });

        Button exportCsvButton = new Button("Export CSV");
        exportCsvButton.setOnAction(event -> exportReport(true));

        Button exportPdfButton = new Button("Export PDF");
        exportPdfButton.setOnAction(event -> exportReport(false));

        panel.getChildren().addAll(selectors, refreshButton, summaryChart, exportCsvButton, exportPdfButton);
        VBox.setVgrow(summaryChart, Priority.ALWAYS);

        return panel;
    }

    private void bindViewModel() {
        viewModel.selectedExpenseProperty().addListener((obs, oldExpense, newExpense) -> {
            if (newExpense != null) {
                populateForm(newExpense);
            }
        });

        viewModel.monthlySummaryProperty().addListener((obs, oldSummary, newSummary) -> {
            if (newSummary != null) {
                updateChart(newSummary);
            } else {
                summaryChart.getData().clear();
            }
        });
    }

    private void loadInitialData() {
        loadExpenses();
        loadSummary();
    }

    private void loadExpenses() {
        Task<List<Expense>> task = new Task<>() {
            @Override
            protected List<Expense> call() throws Exception {
                return apiClient.fetchExpenses();
            }
        };
        task.setOnSucceeded(event -> viewModel.replaceExpenses(task.getValue()));
        task.setOnFailed(event -> showError("Failed to load expenses", task.getException()));
        executor.submit(task);
    }

    private void loadSummary() {
        Integer year = yearSelector.getValue();
        Integer month = monthSelector.getValue();
        if (year == null || month == null) return;

        Task<MonthlySummary> task = new Task<>() {
            @Override
            protected MonthlySummary call() throws Exception {
                return apiClient.fetchMonthlySummary(year, month);
            }
        };
        task.setOnSucceeded(event -> viewModel.setMonthlySummary(task.getValue()));
        task.setOnFailed(event -> showError("Failed to load monthly summary", task.getException()));
        executor.submit(task);
    }

    private void onSave() {
        ExpensePayload payload = buildPayload();
        if (payload == null) return;

        Expense selected = viewModel.getSelectedExpense();
        Task<Expense> task = new Task<>() {
            @Override
            protected Expense call() throws Exception {
                if (selected == null || selected.getId() == null) {
                    return apiClient.createExpense(payload);
                } else {
                    return apiClient.updateExpense(selected.getId(), payload);
                }
            }
        };
        task.setOnSucceeded(event -> {
            clearForm();
            loadExpenses();
            loadSummary();
        });
        task.setOnFailed(event -> showError("Failed to save expense", task.getException()));
        executor.submit(task);
    }

    private void onDelete() {
        Expense selected = viewModel.getSelectedExpense();
        if (selected == null || selected.getId() == null) {
            showError("No expense selected", null);
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                apiClient.deleteExpense(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            clearForm();
            loadExpenses();
            loadSummary();
        });
        task.setOnFailed(event -> showError("Failed to delete expense", task.getException()));
        executor.submit(task);
    }

    private void exportReport(boolean csv) {
        Integer year = yearSelector.getValue();
        Integer month = monthSelector.getValue();
        if (year == null || month == null) {
            showError("Select a year and month first", null);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(csv ? "Save CSV" : "Save PDF");
        chooser.setInitialFileName("expenses-" + YearMonth.of(year, month) + (csv ? ".csv" : ".pdf"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                csv ? "CSV Files" : "PDF Files",
                csv ? "*.csv" : "*.pdf"
        ));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return csv ? apiClient.downloadCsv(year, month) : apiClient.downloadPdf(year, month);
            }
        };
        task.setOnSucceeded(event -> {
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(task.getValue());
            } catch (IOException ex) {
                showError("Failed to save file", ex);
            }
        });
        task.setOnFailed(event -> showError("Failed to export report", task.getException()));
        executor.submit(task);
    }

    private ExpensePayload buildPayload() {
        String title = titleField.getText();
        Category category = categoryCombo.getValue();
        String amountText = amountField.getText();
        LocalDate date = datePicker.getValue();
        String notes = notesArea.getText();

        if (title == null || title.isBlank()) {
            showError("Title is required", null);
            return null;
        }
        if (category == null) {
            showError("Category is required", null);
            return null;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("Amount must be greater than zero");
            }
        } catch (NumberFormatException ex) {
            showError("Enter a valid amount", ex);
            return null;
        }

        if (date == null) {
            showError("Date is required", null);
            return null;
        }

        return new ExpensePayload(title, category, amount, date, notes);
    }

    private void populateForm(Expense expense) {
        titleField.setText(expense.getTitle());
        categoryCombo.setValue(expense.getCategory());
        amountField.setText(expense.getAmount() != null ? expense.getAmount().toPlainString() : "");
        datePicker.setValue(expense.getDate());
        notesArea.setText(expense.getNotes());
    }

    private void clearForm() {
        viewModel.setSelectedExpense(null);
        expenseTable.getSelectionModel().clearSelection();
        titleField.clear();
        categoryCombo.getSelectionModel().select(Category.OTHER);
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        notesArea.clear();
    }

    private void updateChart(MonthlySummary summary) {
        summaryChart.getData().clear();
        if (summary.getCategories() == null) return;

        for (CategorySummary categorySummary : summary.getCategories()) {
            PieChart.Data slice = new PieChart.Data(
                    categorySummary.getCategory().name() + " (" + categorySummary.getTotalAmount() + ")",
                    categorySummary.getTotalAmount().doubleValue()
            );
            summaryChart.getData().add(slice);
        }
    }

    private void showError(String message, Throwable throwable) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(message);
            if (throwable != null) {
                alert.setContentText(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
            }
            alert.showAndWait();
        });
    }

    public void shutdown() {
        executor.shutdown(); // graceful shutdown
    }
}
