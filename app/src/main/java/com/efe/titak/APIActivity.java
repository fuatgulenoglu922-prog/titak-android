package com.efe.titak;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.efe.titak.adapter.APIKeyAdapter;
import com.efe.titak.database.DatabaseHelper;
import com.efe.titak.model.APIKey;

import java.util.List;

public class APIActivity extends AppCompatActivity {

    private ListView lvAPIKeys;
    private EditText etName, etKey, etBaseURL, etModel;
    private Button btnAdd, btnClear;
    private APIKeyAdapter adapter;
    private List<APIKey> apiKeys;
    private DatabaseHelper databaseHelper;
    private APIManager apiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        lvAPIKeys = findViewById(R.id.lv_api_keys);
        etName = findViewById(R.id.et_api_name);
        etKey = findViewById(R.id.et_api_key);
        etBaseURL = findViewById(R.id.et_api_base_url);
        etModel = findViewById(R.id.et_api_model);
        btnAdd = findViewById(R.id.btn_add_api);
        btnClear = findViewById(R.id.btn_clear);

        databaseHelper = new DatabaseHelper(this);
        apiManager = new APIManager(this);

        loadAPIKeys();

        btnAdd.setOnClickListener(v -> addAPIKey());
        btnClear.setOnClickListener(v -> clearFields());
    }

    private void loadAPIKeys() {
        apiKeys = databaseHelper.getAllAPIKeys();
        adapter = new APIKeyAdapter(this, apiKeys, databaseHelper, apiManager);
        lvAPIKeys.setAdapter(adapter);
    }

    private void addAPIKey() {
        String name = etName.getText().toString().trim();
        String apiKey = etKey.getText().toString().trim();
        String baseUrl = etBaseURL.getText().toString().trim();
        String model = etModel.getText().toString().trim();

        if (name.isEmpty() || apiKey.isEmpty()) {
            Toast.makeText(this, "İsim ve API Anahtarı zorunludur", Toast.LENGTH_SHORT).show();
            return;
        }

        if (baseUrl.isEmpty()) {
            baseUrl = "https://api.openai.com/v1";
        }

        if (model.isEmpty()) {
            model = "gpt-3.5-turbo";
        }

        apiManager.addAPIKey(name, apiKey, baseUrl, model);
        Toast.makeText(this, "API eklendi. Sağlayıcı (OpenAI/Claude/Gemini) kullanımda otomatik tespit edilir.", Toast.LENGTH_LONG).show();
        clearFields();
        loadAPIKeys();
    }

    private void clearFields() {
        etName.setText("");
        etKey.setText("");
        etBaseURL.setText("");
        etModel.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAPIKeys();
    }
}
