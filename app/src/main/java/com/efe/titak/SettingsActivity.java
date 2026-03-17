package com.efe.titak;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_RANDOM = 101;
    private static final int PERMISSION_REQUEST = 200;

    private BackgroundManager backgroundManager;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backgroundManager = new BackgroundManager(this);

        EditText etPassword = findViewById(R.id.et_new_password);
        Button btnSave = findViewById(R.id.btn_save_password);
        Button btnDisable = findViewById(R.id.btn_disable_password);

        Button btnPickGallery = findViewById(R.id.btn_pick_gallery);
        Button btnRandomInternet = findViewById(R.id.btn_random_internet);
        Button btnClearBackground = findViewById(R.id.btn_clear_background);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean granted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (granted != null && granted) {
                        backgroundManager.pickFromGallery(this, REQUEST_GALLERY);
                    } else {
                        Toast.makeText(this, "İzin gerekli!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (btnPickGallery != null) {
            btnPickGallery.setOnClickListener(v -> checkPermissionAndPickGallery());
        }

        if (btnRandomInternet != null) {
            btnRandomInternet.setOnClickListener(v -> {
                backgroundManager.downloadRandomFromInternet(this, REQUEST_RANDOM);
            });
        }

        if (btnClearBackground != null) {
            btnClearBackground.setOnClickListener(v -> {
                backgroundManager.clearBackground();
                Toast.makeText(this, "Arka plan kaldırıldı", Toast.LENGTH_SHORT).show();
                recreate();
            });
        }

        btnSave.setOnClickListener(v -> {
            String pass = etPassword.getText().toString().trim();
            if (pass.isEmpty()) {
                Toast.makeText(this, "Sifre bos olamaz!", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("bot_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("app_password", pass)
                    .putBoolean("password_enabled", true)
                    .apply();
            Toast.makeText(this, "Sifre kaydedildi ve aktif edildi.", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(R.anim.theme_enter_reverse, R.anim.theme_exit_reverse);
        });

        btnDisable.setOnClickListener(v -> {
            getSharedPreferences("bot_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("password_enabled", false)
                    .apply();
            Toast.makeText(this, "Sifre korumasi kaldirildi.", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(R.anim.theme_enter_reverse, R.anim.theme_exit_reverse);
        });
    }

    private void checkPermissionAndPickGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                return;
            }
        }
        backgroundManager.pickFromGallery(this, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    backgroundManager.setBackgroundFromUri(this, imageUri);
                }
            } else if (requestCode == REQUEST_RANDOM) {
                backgroundManager.applyBackground(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundManager.applyBackground(this);
    }
}
