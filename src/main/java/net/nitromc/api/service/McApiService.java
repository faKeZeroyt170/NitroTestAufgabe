package net.nitromc.api.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class McApiService {

    private static final String BASE_URL = "https://mc-api.io";

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public McApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public CompletableFuture<JsonObject> getPlayerProfile(String playerName) {

        String url = BASE_URL + "/profile/" + playerName + "/JAVA";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient
                .sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenCompose(response -> {

                    if (response.statusCode() < 200
                            || response.statusCode() >= 300) {

                        return CompletableFuture.failedFuture(
                                new ApiException(
                                        response.statusCode(),
                                        getErrorMessage(response.body())
                                )
                        );
                    }

                    try {
                        JsonObject json = JsonParser
                                .parseString(response.body())
                                .getAsJsonObject();

                        return CompletableFuture.completedFuture(json);

                    } catch (Exception exception) {

                        return CompletableFuture.failedFuture(
                                new ApiException(
                                        500,
                                        "Ungültige Antwort der API."
                                )
                        );
                    }
                });
    }

    private String getErrorMessage(String responseBody) {

        try {
            JsonObject json = JsonParser
                    .parseString(responseBody)
                    .getAsJsonObject();

            if (json.has("message")) {
                return json.get("message").getAsString();
            }

            if (json.has("error")) {
                return json.get("error").getAsString();
            }

        } catch (Exception ignored) {
        }

        return "Die API konnte die Anfrage nicht verarbeiten.";
    }

    public static class ApiException extends RuntimeException {

        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}