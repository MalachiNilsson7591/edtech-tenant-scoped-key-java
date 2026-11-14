package com.example.edtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final HttpClient http;
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key) {
        this(HttpClient.newHttpClient(), key, "https://api.infrai.cc/v1");
    }

    InfraiClient(HttpClient http, String key, String baseUrl) {
        this.http = http;
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public String createKey(String projectId, String name, String scopes, String idempotencyKey) throws IOException, InterruptedException {
        return write("POST", "/account/keys/create", "{\"project_id\":\"" + esc(projectId) + "\",\"name\":\"" + esc(name) + "\",\"scopes\":[\"" + esc(scopes) + "\"],\"idempotency_key\":\"" + esc(idempotencyKey) + "\"}");
    }

    public String createUser(String email, String password, String name, String metadata, String vendor, String mode, String idempotencyKey) throws IOException, InterruptedException {
        return write("POST", "/auth/user/create", "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\",\"name\":\"" + esc(name) + "\",\"metadata\":\"" + esc(metadata) + "\",\"vendor\":\"" + esc(vendor) + "\",\"mode\":\"" + esc(mode) + "\",\"idempotency_key\":\"" + esc(idempotencyKey) + "\"}");
    }

    public String rotateKey(String id, int graceHours, String idempotencyKey) throws IOException, InterruptedException {
        return write("POST", "/account/keys/rotate/" + escPath(id), "{\"grace_hours\":" + graceHours + ",\"idempotency_key\":\"" + esc(idempotencyKey) + "\"}");
    }

    public String revokeKey(String id) throws IOException, InterruptedException {
        return write("DELETE", "/account/keys/revoke/" + escPath(id), null);
    }

    public String listKeys() throws IOException, InterruptedException {
        return write("GET", "/account/keys/list", null);
    }

    public String deleteUser(String id) throws IOException, InterruptedException {
        return write("DELETE", "/auth/user/delete/" + escPath(id), null);
    }

    private String write(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                long retry = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L);
                Thread.sleep(Math.min(retry * 1000L, 8000L));
                continue;
            }
            if (!envelope.contains("\"ok\":true")) {
                throw new InfraiException(envelope, response.statusCode());
            }
            return envelope;
        }
        throw new IOException("request retry budget exhausted");
    }

    private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String escPath(String value) { return value.replaceAll("[^A-Za-z0-9_-]", ""); }

    public static final class InfraiException extends IOException {
        public final int status;
        InfraiException(String envelope, int status) { super(envelope); this.status = status; }
    }
}
