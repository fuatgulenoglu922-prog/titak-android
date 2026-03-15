package com.efe.titak.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.efe.titak.APIManager;
import com.efe.titak.R;
import com.efe.titak.database.DatabaseHelper;
import com.efe.titak.model.APIKey;

import java.util.List;

public class APIKeyAdapter extends ArrayAdapter<APIKey> {

    private final Context context;
    private final List<APIKey> apiKeys;
    private final DatabaseHelper databaseHelper;
    private final APIManager apiManager;

    public APIKeyAdapter(@NonNull Context context, List<APIKey> apiKeys, DatabaseHelper databaseHelper, APIManager apiManager) {
        super(context, R.layout.item_api_key, apiKeys);
        this.context = context;
        this.apiKeys = apiKeys;
        this.databaseHelper = databaseHelper;
        this.apiManager = apiManager;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_api_key, parent, false);
        }

        APIKey apiKey = apiKeys.get(position);

        TextView tvName = view.findViewById(R.id.tv_api_name);
        TextView tvBaseURL = view.findViewById(R.id.tv_api_base_url);
        TextView tvModel = view.findViewById(R.id.tv_api_model);
        Button btnSet = view.findViewById(R.id.btn_set_active);
        Button btnDelete = view.findViewById(R.id.btn_delete);

        tvName.setText(apiKey.getName());
        tvBaseURL.setText(apiKey.getBaseUrl());
        tvModel.setText("Model: " + apiKey.getModel());

        if (apiKey.isActive()) {
            btnSet.setText("Aktif");
            btnSet.setEnabled(false);
            btnSet.setBackgroundColor(context.getColor(R.color.accent_green));
        } else {
            btnSet.setText("Aktif Et");
            btnSet.setEnabled(true);
            btnSet.setOnClickListener(v -> {
                apiManager.setActiveAPIKey(apiKey.getId());
                notifyDataSetChanged();
            });
        }

        btnDelete.setOnClickListener(v -> {
            databaseHelper.deleteAPIKey(apiKey.getId());
            apiKeys.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "API anahtarı silindi", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
