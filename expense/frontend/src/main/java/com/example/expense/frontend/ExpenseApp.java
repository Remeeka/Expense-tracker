package com.example.expense.frontend;

import com.example.expense.frontend.api.ExpenseApiClient;
import com.example.expense.frontend.config.AppSettings;
import com.example.expense.frontend.ui.MainView;
import com.example.expense.frontend.viewmodel.ExpenseViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ExpenseApp extends Application {

    private MainView mainView;

    @Override
    public void start(Stage stage) {
        AppSettings settings = new AppSettings();
        ExpenseApiClient apiClient = new ExpenseApiClient(settings.getBackendBaseUrl());
        ExpenseViewModel viewModel = new ExpenseViewModel();

        mainView = new MainView(apiClient, viewModel);
        Scene scene = new Scene(mainView, 1100, 600);
        stage.setTitle("Daily Expense Manager");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (mainView != null) {
            mainView.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

