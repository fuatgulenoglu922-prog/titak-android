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

        // Play Games Giriş Kontrolü
        checkPlayGamesSignIn();

        // 4 saniye sonra otomatik geçiş yap
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Logic handled by checkPlayGamesSignIn potentially, 
            // but we ensure it proceeds after 2s if authenticated or not.
            if (!isFinishing()) proceedToNext();
        }, 2000);
    }

    private void checkPlayGamesSignIn() {
        GamesSignInClient signInClient = PlayGames.getGamesSignInClient(this);
        signInClient.isAuthenticated().addOnCompleteListener(task -> {
            boolean isAuthenticated = (task.isSuccessful() && task.getResult().isAuthenticated());
            if (isAuthenticated) {
                // Giriş yapılmış, SocialManager ile senkronize et
                signInClient.requestServerSideAccess("", false).addOnCompleteListener(accessTask -> {
                    // Normalde burada auth token alınıp Firebase'e gönderilir
                    // Basitlik için sadece devam ediyoruz
                    SocialManager.getInstance().syncUserProfile("GP_ID_LINKED", "Oyuncu", null);
                    proceedToNext();
                });
            } else {
                // Giriş yapılmamış, yine de devam et ama social kısıtlı kalacak
                proceedToNext();
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
