package com.efe.titak;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

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

        // 4 saniye sonra otomatik geçiş yap
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            proceedToNext();
        }, 4000);
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
