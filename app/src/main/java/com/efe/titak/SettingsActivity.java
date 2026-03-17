package com.efe.titak;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText etPassword = findViewById(R.id.et_new_password);
        Button btnSave = findViewById(R.id.btn_save_password);
        Button btnDisable = findViewById(R.id.btn_disable_password);


        btnSave.setOnClickListener(v -> {
            String pass = etPassword.getText().toString().trim();
            if (pass.isEmpty()) {
                Toast.makeText(this, "Şifre boş olamaz!", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("bot_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("app_password", pass)
                    .putBoolean("password_enabled", true)
                    .apply();
            Toast.makeText(this, "Şifre kaydedildi ve aktif edildi.", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(R.anim.theme_enter_reverse, R.anim.theme_exit_reverse);
        });

        btnDisable.setOnClickListener(v -> {
            getSharedPreferences("bot_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("password_enabled", false)
                    .apply();
            Toast.makeText(this, "Şifre koruması kaldırıldı.", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(R.anim.theme_enter_reverse, R.anim.theme_exit_reverse);
        });


    }
}
