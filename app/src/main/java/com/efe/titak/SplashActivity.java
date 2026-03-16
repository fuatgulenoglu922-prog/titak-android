package com.efe.titak;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Müzik çalmaya başla
        MusicManager.getInstance(this).playMusic();

        TextView tvVersion = findViewById(R.id.tv_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("v" + pInfo.versionName);
        } catch (Exception e) {
            tvVersion.setText("v2.8");
        }

        // Kullanıcıya özel mesaj
        TextView tvMessage = findViewById(R.id.tv_message);
        tvMessage.setText("Uygulama Ali Sağlam tarafından yapılmıştır\nProfosyone kod ve metin yazarı");

        // Buton tıklaması ile devam et
        Button btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> {
            // Şifre koruması kontrolü
            boolean passEnabled = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("password_enabled", false);
            if (passEnabled) {
                startActivity(new Intent(SplashActivity.this, LockActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        });
    }
}
