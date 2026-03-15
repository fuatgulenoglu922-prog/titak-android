package com.efe.titak;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.efe.titak.database.DatabaseHelper;
import com.efe.titak.model.APIKey;

import java.util.List;

public class APIManager {
    private static final String PREFS_NAME = "api_manager";
    private static final String ACTIVE_API_ID = "active_api_id";

    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final SharedPreferences sharedPreferences;

    public APIManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public APIKey getActiveAPIKey() {
        int activeId = sharedPreferences.getInt(ACTIVE_API_ID, -1);
        if (activeId != -1) {
            return databaseHelper.getAPIKey(activeId);
        }
        return null;
    }

    public void setActiveAPIKey(int id) {
        databaseHelper.setActiveAPIKey(id);
        sharedPreferences.edit().putInt(ACTIVE_API_ID, id).apply();
        Toast.makeText(context, "API anahtarı seçildi", Toast.LENGTH_SHORT).show();
    }

    public List<APIKey> getAllAPIKeys() {
        return databaseHelper.getAllAPIKeys();
    }

    public long addAPIKey(String name, String apiKey, String baseUrl, String model) {
        APIKey key = new APIKey(0, name, apiKey, baseUrl, model, System.currentTimeMillis(), false);
        long id = databaseHelper.addAPIKey(key);
        if (id != -1) {
            Toast.makeText(context, "API anahtarı eklendi", Toast.LENGTH_SHORT).show();
        }
        return id;
    }

    public void updateAPIKey(APIKey apiKey) {
        databaseHelper.updateAPIKey(apiKey);
        Toast.makeText(context, "API anahtarı güncellendi", Toast.LENGTH_SHORT).show();
    }

    public void deleteAPIKey(int id) {
        databaseHelper.deleteAPIKey(id);
        Toast.makeText(context, "API anahtarı silindi", Toast.LENGTH_SHORT).show();
    }

    public boolean hasActiveAPI() {
        return getActiveAPIKey() != null;
    }
}
