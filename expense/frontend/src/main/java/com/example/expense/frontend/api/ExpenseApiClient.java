package com.example.expense.frontend.api;

import com.example.expense.frontend.dto.ExpensePayload;
import com.example.expense.frontend.model.Expense;
import com.example.expense.frontend.model.MonthlySummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExpenseApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ExpenseApiClient(String baseUrl) {
        this.baseUrl = Optional.ofNullable(baseUrl)
                .filter(url -> !url.isBlank())
                .orElse("http://localhost:8080");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public List<Expense> fetchExpenses() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return objectMapper.readValue(response.body(), new TypeReference<List<Expense>>() {
        });
    }

    public Expense createExpense(ExpensePayload payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return objectMapper.readValue(response.body(), Expense.class);
    }

    public Expense updateExpense(String id, ExpensePayload payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return objectMapper.readValue(response.body(), Expense.class);
    }

    public void deleteExpense(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses/" + id))
                .DELETE()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        ensureSuccess(response);
    }

    public MonthlySummary fetchMonthlySummary(int year, int month) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses/summary/monthly", Map.of(
                        "year", String.valueOf(year),
                        "month", String.valueOf(month)
                )))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return objectMapper.readValue(response.body(), MonthlySummary.class);
    }

    public byte[] downloadCsv(int year, int month) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses/export/csv", Map.of(
                        "year", String.valueOf(year),
                        "month", String.valueOf(month)
                )))
                .header("Accept", "text/csv")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response);
        return response.body();
    }

    public byte[] downloadPdf(int year, int month) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/api/expenses/export/pdf", Map.of(
                        "year", String.valueOf(year),
                        "month", String.valueOf(month)
                )))
                .header("Accept", "application/pdf")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response);
        return response.body();
    }

    private URI buildUri(String path) {
        return URI.create(baseUrl + path);
    }

    private URI buildUri(String path, Map<String, String> params) {
        if (params.isEmpty()) {
            return buildUri(path);
        }
        String query = params.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return URI.create(baseUrl + path + "?" + query);
    }

    private void ensureSuccess(HttpResponse<?> response) throws IOException {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Request failed with status " + status);
        }
    }
}

