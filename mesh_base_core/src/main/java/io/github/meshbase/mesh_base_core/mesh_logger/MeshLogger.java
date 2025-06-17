package io.github.meshbase.mesh_base_core.mesh_logger;

import com.google.gson.Gson;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class MeshLogger {
    private final String baseUrl;
    private final ExecutorService executor;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private boolean debug;

    public MeshLogger(String baseUrl, ExecutorService executor, OkHttpClient httpClient, boolean debug) {
        this.baseUrl = baseUrl;
        this.executor = executor;
        this.httpClient = httpClient;
        this.debug = debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebugMode() {
        return debug;
    }

    public void sendEvent(MeshEventsModel event) {
        if (debug) {
            executor.submit(() -> {
                try {
                    String json = gson.toJson(event);
                    RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
                    Request request = new Request.Builder().url(baseUrl).post(body).build();
                    httpClient.newCall(request).execute().close(); // no need to read body
                } catch (IOException e) {
                    System.err.println("Failed to send event: " + e.getMessage());
                }
            });
        } else {
            System.out.println("[MeshLogger Debug Event] " + event.source_id + event.event_type);
        }
    }
}
