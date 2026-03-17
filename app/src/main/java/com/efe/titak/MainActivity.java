package com.efe.titak;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.projection.MediaProjectionManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    private TextView tvVersion;
    private Button btnMusicToggle;
    private MusicManager musicManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvVersion = findViewById(R.id.tv_version);
        btnMusicToggle = findViewById(R.id.btn_music_toggle);
        musicManager = MusicManager.getInstance(this);

        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + v);
        } catch (Exception e) {
            tvVersion.setText("v3.2");
        }

        // Pro Özellikler Butonu
        findViewById(R.id.btn_pro_features).setOnClickListener(v -> showProPasswordDialog());

        // Güncelleme butonu
        findViewById(R.id.btn_update_app).setOnClickListener(v -> {
            UpdateManager updateManager = new UpdateManager(this);
            updateManager.checkAndInstallUpdate();
        });

        // Ayarlar Butonu
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.theme_enter, R.anim.theme_exit);
        });

        // Yapay Zeka API Butonu
        // findViewById(R.id.btn_api).setOnClickListener(v -> startActivity(new Intent(this, APIActivity.class)));

        // Geri Bildirim Butonu
        findViewById(R.id.btn_feedback).setOnClickListener(v -> {
            startActivity(new Intent(this, FeedbackActivity.class));
        });

        // Müzik Kontrol Butonu
        updateMusicButton();
        btnMusicToggle.setOnClickListener(v -> {
            musicManager.toggleMusic();
            updateMusicButton();
        });

        // Müziği otomatik başlat
        boolean autoPlayMusic = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("auto_play_music", true);
        if (autoPlayMusic) {
            musicManager.playMusic();
            updateMusicButton();
        }

        // Sosyal Butonlar
        findViewById(R.id.btn_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.btn_social_hub).setOnClickListener(v -> {
            startActivity(new Intent(this, SocialActivity.class));
        });
    }

    private void showProPasswordDialog() {
        android.widget.EditText etPassword = new android.widget.EditText(this);
        etPassword.setHint("Şifre yazınız");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new android.app.AlertDialog.Builder(this)
            .setTitle("PRO ERİŞİMİ")
            .setMessage("Lütfen Pro özellikleri açmak için şifreyi girin.")
            .setView(etPassword)
            .setPositiveButton("Giriş", (d, w) -> {
                String pass = etPassword.getText().toString();
                if ("ALİ".equals(pass)) {
                    Toast.makeText(this, "PRO Özellikler Aktif!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, ProSettingsActivity.class));
                } else {
                    Toast.makeText(this, "Yanlış Şifre!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private void updateMusicButton() {
        if (musicManager.isPlaying()) {
            btnMusicToggle.setText("🔊 Müzik: Açık");
        } else {
            btnMusicToggle.setText("🔇 Müzik: Kapalı");
        }
    }
}
