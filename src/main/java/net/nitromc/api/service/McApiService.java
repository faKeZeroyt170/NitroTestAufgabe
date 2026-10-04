package net.nitromc.api.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.nitromc.api.model.NameHistoryEntry;
import net.nitromc.api.model.PlayerProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class McApiService {

    private static final String BASE_URL = "https://mc-api.io";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public McApiService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public CompletableFuture<PlayerProfile> getPlayerProfile(
            String playerName
    ) {

        String url = BASE_URL
                + "/profile/"
                + playerName
                + "/JAVA";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenCompose(response -> {

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

                JsonObject json =
                        JsonParser.parseString(response.body())
                                .getAsJsonObject();

                String uuid = null;
                String name = null;
                String skinUrl = null;

                if (json.has("uuid")
                        && !json.get("uuid").isJsonNull()) {

                    uuid = json.get("uuid").getAsString();
                }

                if (json.has("name")
                        && !json.get("name").isJsonNull()) {

                    name = json.get("name").getAsString();
                }

                if (json.has("decodedTexture")
                        && json.get("decodedTexture").isJsonObject()) {

                    JsonObject decodedTexture =
                            json.getAsJsonObject("decodedTexture");

                    if (decodedTexture.has("textures")
                            && decodedTexture.get("textures").isJsonObject()) {

                        JsonObject textures =
                                decodedTexture.getAsJsonObject("textures");

                        if (textures.has("SKIN")
                                && textures.get("SKIN").isJsonObject()) {

                            JsonObject skin =
                                    textures.getAsJsonObject("SKIN");

                            if (skin.has("url")
                                    && !skin.get("url").isJsonNull()) {

                                skinUrl = skin.get("url").getAsString();
                            }
                        }
                    }
                }

                Map<String, Long> names = new LinkedHashMap<>();

                if (json.has("names")
                        && json.get("names").isJsonObject()) {

                    JsonObject nameHistory =
                            json.getAsJsonObject("names");

                    nameHistory.entrySet().forEach(entry -> {

                        try {
                            names.put(
                                    entry.getKey(),
                                    entry.getValue().getAsLong()
                            );
                        } catch (Exception ignored) {
                        }
                    });
                }

                return CompletableFuture.completedFuture(
                        new PlayerProfile(
                                uuid,
                                name,
                                skinUrl,
                                names
                        )
                );

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

    public CompletableFuture<List<NameHistoryEntry>> getNameHistory(
            String uuid
    ) {

        String url = BASE_URL
                + "/api/v1/name-history/"
                + uuid;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenCompose(response -> {

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

                JsonObject json =
                        JsonParser.parseString(response.body())
                                .getAsJsonObject();

                List<NameHistoryEntry> entries =
                        new ArrayList<>();

                if (json.has("names")
                        && json.get("names").isJsonArray()) {

                    JsonArray names =
                            json.getAsJsonArray("names");

                    names.forEach(element -> {

                        if (!element.isJsonObject()) {
                            return;
                        }

                        JsonObject entry =
                                element.getAsJsonObject();

                        String name = null;
                        long changedAt = 0;

                        if (entry.has("name")
                                && !entry.get("name").isJsonNull()) {

                            name = entry.get("name").getAsString();
                        }

                        if (entry.has("changedAt")
                                && !entry.get("changedAt").isJsonNull()) {

                            changedAt = entry.get("changedAt").getAsLong();

                        } else if (entry.has("timestamp")
                                && !entry.get("timestamp").isJsonNull()) {

                            changedAt = entry.get("timestamp").getAsLong();
                        }

                        if (name != null) {

                            entries.add(
                                    new NameHistoryEntry(
                                            name,
                                            changedAt
                                    )
                            );
                        }
                    });
                }

                return CompletableFuture.completedFuture(entries);

            } catch (Exception exception) {

                return CompletableFuture.failedFuture(
                        new ApiException(
                                500,
                                "Ungültige Antwort der Namenshistorie."
                        )
                );
            }
        });
    }

    private String getErrorMessage(String responseBody) {

        try {

            JsonObject json =
                    JsonParser.parseString(responseBody)
                            .getAsJsonObject();

            if (json.has("message")
                    && !json.get("message").isJsonNull()) {

                return json.get("message").getAsString();
            }

            if (json.has("error")
                    && !json.get("error").isJsonNull()) {

                return json.get("error").getAsString();
            }

        } catch (Exception ignored) {
        }

        return "Die API konnte die Anfrage nicht verarbeiten.";
    }

    public static class ApiException extends RuntimeException {

        private final int statusCode;

        public ApiException(
                int statusCode,
                String message
        ) {
            super(message);

            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}