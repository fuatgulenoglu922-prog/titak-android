package com.efe.titak;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.efe.titak.manager.SocialManager;

public class SplashActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private BackgroundManager backgroundManager;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CAMERA
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        backgroundManager = new BackgroundManager(this);
        // backgroundManager.applyBackground(this); // Atma sorununa yol açabilir, şimdilik kapalı

        // Müzik çalmaya başla
        try {
            MusicManager.getInstance(this).playMusic();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Kurt uluma sesini çal
        try {
            int resId = getResources().getIdentifier("wolf_howl", "raw", getPackageName());
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // İzinleri kontrol et ve iste
        if (!hasPermissions()) {
            startFlow(); // İzin diyaloğu bazen takılmaya sebep olur, doğrudan akışı başlat
        } else {
            startFlow();
        }
    }

    private boolean hasPermissions() {
        try {
            for (String permission : REQUIRED_PERMISSIONS) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    private void startFlow() {
        // 2 saniye sonra otomatik geçiş yap
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) proceedToNext();
        }, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            startFlow();
        }
    }

    private void proceedToNext() {
        try {
            // Şifre koruması kontrolü
            boolean passEnabled = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("password_enabled", false);
            if (passEnabled) {
                startActivity(new Intent(SplashActivity.this, LockActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        } catch (Exception e) {
            // Hata olursa en azından MainActivity'yi açmayı dene
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
    }
}
