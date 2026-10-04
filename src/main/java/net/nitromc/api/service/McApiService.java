package net.nitromc.api.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.nitromc.api.model.PlayerProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
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

    /**
     * Lädt die Profilinformationen eines Minecraft-Spielers.
     *
     * Die Anfrage läuft vollständig asynchron.
     *
     * @param playerName Minecraft-Spielername
     * @return CompletableFuture mit dem PlayerProfile
     */
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

        return httpClient
                .sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenCompose(response -> {

                    /*
                     * Nur erfolgreiche HTTP-Statuscodes akzeptieren.
                     */
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
                                JsonParser
                                        .parseString(response.body())
                                        .getAsJsonObject();

                        /*
                         * UUID
                         */
                        String uuid = null;

                        if (json.has("uuid")
                                && !json.get("uuid").isJsonNull()) {

                            uuid = json
                                    .get("uuid")
                                    .getAsString();
                        }

                        /*
                         * Aktueller Name
                         */
                        String name = null;

                        if (json.has("name")
                                && !json.get("name").isJsonNull()) {

                            name = json
                                    .get("name")
                                    .getAsString();
                        }

                        /*
                         * Skin
                         */
                        String skinUrl = null;

                        if (json.has("decodedTexture")
                                && json.get("decodedTexture").isJsonObject()) {

                            JsonObject decodedTexture =
                                    json.getAsJsonObject(
                                            "decodedTexture"
                                    );

                            if (decodedTexture.has("textures")
                                    && decodedTexture
                                    .get("textures")
                                    .isJsonObject()) {

                                JsonObject textures =
                                        decodedTexture
                                                .getAsJsonObject("textures");

                                if (textures.has("SKIN")
                                        && textures
                                        .get("SKIN")
                                        .isJsonObject()) {

                                    JsonObject skin =
                                            textures
                                                    .getAsJsonObject("SKIN");

                                    if (skin.has("url")
                                            && !skin
                                            .get("url")
                                            .isJsonNull()) {

                                        skinUrl = skin
                                                .get("url")
                                                .getAsString();
                                    }
                                }
                            }
                        }

                        /*
                         * Namenshistorie
                         */
                        Map<String, Long> names =
                                new LinkedHashMap<>();

                        if (json.has("names")
                                && json.get("names").isJsonObject()) {

                            JsonObject nameHistory =
                                    json.getAsJsonObject("names");

                            nameHistory.entrySet().forEach(entry -> {

                                try {

                                    names.put(
                                            entry.getKey(),
                                            entry.getValue()
                                                    .getAsLong()
                                    );

                                } catch (Exception ignored) {
                                    // Ungültigen Eintrag überspringen.
                                }
                            });
                        }

                        /*
                         * Model erstellen
                         */
                        PlayerProfile profile =
                                new PlayerProfile(
                                        uuid,
                                        name,
                                        skinUrl,
                                        names
                                );

                        return CompletableFuture.completedFuture(
                                profile
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

    /**
     * Liest eine Fehlermeldung aus der API-Antwort.
     */
    private String getErrorMessage(String responseBody) {

        try {

            JsonObject json =
                    JsonParser
                            .parseString(responseBody)
                            .getAsJsonObject();

            if (json.has("message")
                    && !json.get("message").isJsonNull()) {

                return json
                        .get("message")
                        .getAsString();
            }

            if (json.has("error")
                    && !json.get("error").isJsonNull()) {

                return json
                        .get("error")
                        .getAsString();
            }

        } catch (Exception ignored) {
            // Keine gültige JSON-Fehlermeldung.
        }

        return "Die API konnte die Anfrage nicht verarbeiten.";
    }

    /**
     * Eigene Exception für API-Fehler.
     */
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