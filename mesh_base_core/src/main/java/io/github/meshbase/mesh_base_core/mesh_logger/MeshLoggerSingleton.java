package io.github.meshbase.mesh_base_core.mesh_logger;

import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class MeshLoggerSingleton {
    private static volatile MeshLogger instance;

    private MeshLoggerSingleton() {
    }

    public static MeshLogger get() {
        if (instance == null) {
            synchronized (MeshLoggerSingleton.class) {
                if (instance == null) {
                    instance = new MeshLogger(
                            "http://localhost:8000/event",
                            Executors.newCachedThreadPool(),
                            new OkHttpClient(),
                            true
                    );
                }
            }
        }

        return instance;
    }

    public static void configure(String baseUrl, boolean debug) {
        synchronized (MeshLoggerSingleton.class) {
            if (instance == null) {
                instance = new MeshLogger(
                        baseUrl,
                        Executors.newCachedThreadPool(),
                        new OkHttpClient(),
                        debug
                );
            } else {
                instance.setDebug(debug);
            }
        }
    }
}