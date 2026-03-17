package com.efe.titak;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.efe.titak.manager.SocialManager;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

public class SplashActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

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

        // Müzik çalmaya başla
        MusicManager.getInstance(this).playMusic();

        // Kurt uluma sesini çal
        try {
            int resId = getResources().getIdentifier("wolf_howl", "raw", getPackageName());
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Play Games SDK'yı başlat
        PlayGamesSdk.initialize(this);

        // İzinleri kontrol et ve iste
        if (!hasPermissions()) {
            android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
            new android.app.AlertDialog.Builder(this)
                .setTitle("İzin Gerekli")
                .setMessage("Uygulamanın iletişim özelliklerini (sesli arama, bildirim) kullanabilmeniz için izin vermeniz gerekmektedir.")
                .setCancelable(false)
                .setPositiveButton("İzin Ver", (d, w) -> {
                    androidx.core.app.ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQ_ID);
                })
                .show();
        } else {
            startFlow();
        }
    }

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startFlow() {
        // Play Games Giriş Kontrolü
        checkPlayGamesSignIn();

        // 2 saniye sonra otomatik geçiş yap
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) proceedToNext();
        }, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            // İzinler ne olursa olsun devam et (user said "one time")
            startFlow();
        }
    }

    private void checkPlayGamesSignIn() {
        GamesSignInClient signInClient = PlayGames.getGamesSignInClient(this);
        signInClient.isAuthenticated().addOnCompleteListener(task -> {
            boolean isAuthenticated = (task.isSuccessful() && task.getResult().isAuthenticated());
            if (isAuthenticated) {
                signInClient.requestServerSideAccess("", false).addOnCompleteListener(accessTask -> {
                    SocialManager.getInstance().syncUserProfile("GP_ID_LINKED", "Oyuncu", null);
                });
            }
        });
    }

    private void proceedToNext() {
        // Şifre koruması kontrolü
        boolean passEnabled = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("password_enabled", false);
        if (passEnabled) {
            startActivity(new Intent(SplashActivity.this, LockActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
