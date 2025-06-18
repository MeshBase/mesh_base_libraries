package io.github.meshbase.mesh_base_core.mesh_manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.UUID;

import android.util.Base64;

public class Store {
    private static Store instance;
    private final Context context;
    private final String PREFERENCES_KEY = "mesh_prefs";
    private final String ID_KEY = "id";
    private final String PUBLIC_KEY = "public_key";
    private final String PRIVATE_KEY = "private_key";

    private Store(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized Store getInstance(Context context) {
        if (instance == null) {
            instance = new Store(context);
        }
        return instance;
    }

    // Store your own UUID
    public void storeId(UUID id) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(ID_KEY, id.toString());
        editor.apply();
    }

    @Nullable
    public UUID getId() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        String stringId = preferences.getString(ID_KEY, null);
        if (stringId == null) return null;
        return UUID.fromString(stringId);
    }

    /**
     * Store public key for self
     *
     * @param publicKey The public key generated and tobe stored for later acccess
     */
    public void storePublicKey(byte[] publicKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PUBLIC_KEY, Base64.encodeToString(publicKey, Base64.DEFAULT));
        editor.apply();
    }

    /**
     * Get the public key of self
     */
    @Nullable
    public byte[] getPublicKey() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        String publicKeyString = preferences.getString(PUBLIC_KEY, null);
        if (publicKeyString == null) return null;
        return Base64.decode(publicKeyString, Base64.DEFAULT);
    }

    // Store your own private key
    public void storePrivateKey(byte[] privateKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PRIVATE_KEY, Base64.encodeToString(privateKey, Base64.DEFAULT));
        editor.apply();
    }

    @Nullable
    public byte[] getPrivateKey() {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        String privateKeyString = preferences.getString(PRIVATE_KEY, null);
        if (privateKeyString == null) return null;
        return Base64.decode(privateKeyString, Base64.DEFAULT);
    }

    // Store a neighbor's public key (cache)
    public void storeNeighborPublicKey(UUID uuid, byte[] publicKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("neighbor_" + uuid.toString(), Base64.encodeToString(publicKey, Base64.DEFAULT));
        editor.apply();
    }

    public byte[] getNeighborPublicKey(UUID uuid) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        String publicKeyString = preferences.getString("neighbor_" + uuid.toString(), null);
        if (publicKeyString == null) return null;
        return Base64.decode(publicKeyString, Base64.DEFAULT);
    }
}