package com.example.mesh_base.mesh_manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.UUID;


//Separate class so that it can be injected instead of instantiated in MeshManager in the future
public class Store {
  private static Store instance;
  private final Context context;
  private final String PREFERENCES_KEY = "mesh_prefs";
  private final String ID_KEY = "id";

  private Store(Context context) {
    this.context = context.getApplicationContext();
  }

  public static synchronized Store getInstance(Context context) {
    if (instance == null) {
      instance = new Store(context);
    }
    return instance;
  }

  public void storeId(UUID id) {
    SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(ID_KEY, id.toString());
    editor.apply();
  }

  @Nullable()
  public UUID getId() {
    SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    String stringId = preferences.getString(ID_KEY, null);
    if (stringId == null) return null;
    return UUID.fromString(stringId);
  }

}
