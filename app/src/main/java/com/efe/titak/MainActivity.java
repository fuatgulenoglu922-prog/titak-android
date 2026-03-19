package com.efe.titak;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import java.util.concurrent.TimeUnit;
import com.efe.titak.worker.UpdateCheckWorker;
import com.efe.titak.manager.SocialManager;

public class MainActivity extends AppCompatActivity {
    private TextView tvVersion;
    private View btnMusicToggle;
    private MusicManager musicManager;
    private BackgroundManager backgroundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundManager = new BackgroundManager(this);
        // Atma sorununu önlemek için Splash'ten sonra burada güvenli yükleme
        try {
            backgroundManager.applyBackground(this);
        } catch (Exception e) {}

        tvVersion = findViewById(R.id.tv_version);
        btnMusicToggle = findViewById(R.id.btn_music_toggle);
        musicManager = MusicManager.getInstance(this);

        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (tvVersion != null) tvVersion.setText("v" + v);
        } catch (Exception e) {
            if (tvVersion != null) tvVersion.setText("v5.2");
        }

        setupUserSession();

        // Buton Bağlantıları
        View btnLogin = findViewById(R.id.btn_google_login);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);

        View btnInvite = findViewById(R.id.btn_invite_friend);
        if (btnInvite != null) btnInvite.setOnClickListener(v -> shareInviteLink());

        View btnPro = findViewById(R.id.btn_pro_features);
        if (btnPro != null) btnPro.setOnClickListener(v -> showProPasswordDialog());

        View btnUpdate = findViewById(R.id.btn_update_app);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                UpdateManager updateManager = new UpdateManager(this);
                updateManager.checkAndInstallUpdate();
            });
        }

        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }

        View btnFeedback = findViewById(R.id.btn_feedback);
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> {
                startActivity(new Intent(this, FeedbackActivity.class));
            });
        }

        if (btnMusicToggle != null) {
            btnMusicToggle.setOnClickListener(v -> {
                musicManager.toggleMusic();
            });
        }

        View btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }

        View btnSocial = findViewById(R.id.btn_social_hub);
        if (btnSocial != null) {
            btnSocial.setOnClickListener(v -> {
                startActivity(new Intent(this, SocialActivity.class));
            });
        }
    }

    private void setupUserSession() {
        boolean isLocalUser = getSharedPreferences("titak_prefs", MODE_PRIVATE).getBoolean("is_local_user", false);
        if (!isLocalUser) {
            showInitialSetupDialog();
        } else {
            String localName = getSharedPreferences("titak_prefs", MODE_PRIVATE).getString("local_display_name", "Kullanıcı");
            String localId = getSharedPreferences("titak_prefs", MODE_PRIVATE).getString("local_titak_id", null);
            String localUid = getSharedPreferences("titak_prefs", MODE_PRIVATE).getString("local_uid", null);
            SocialManager.getInstance().setupLocalUser(localName, localId, localUid);
        }
    }

    private void showInitialSetupDialog() {
        EditText etUsername = new EditText(this);
        etUsername.setHint("Kullanıcı Adınız");

        new AlertDialog.Builder(this)
            .setTitle("TiTak 5.2")
            .setMessage("Lütfen başlamak için bir isim seçin.")
            .setView(etUsername)
            .setCancelable(false)
            .setPositiveButton("BAŞLA", (d, w) -> {
                String name = etUsername.getText().toString().trim();
                if (name.isEmpty()) name = "Birim_" + new java.util.Random().nextInt(100);
                
                String titakId = String.valueOf(new java.util.Random().nextInt(9000) + 1000);
                String uid = "USER_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                
                getSharedPreferences("titak_prefs", MODE_PRIVATE).edit()
                    .putString("local_display_name", name)
                    .putString("local_titak_id", titakId)
                    .putString("local_uid", uid)
                    .putBoolean("is_local_user", true)
                    .apply();
                    
                SocialManager.getInstance().setupLocalUser(name, titakId, uid);
                Toast.makeText(this, "Hoş geldin " + name, Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void shareInviteLink() {
        String myId = getSharedPreferences("titak_prefs", MODE_PRIVATE).getString("local_titak_id", "");
        String inviteMsg = "TiTak Telsiz Ağına Katıl! ID'm: " + myId + "\nİndir: https://titak.efe.com/invite?id=" + myId;
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, inviteMsg);
        startActivity(Intent.createChooser(sendIntent, "Arkadaşını Davet Et"));
    }

    private void showProPasswordDialog() {
        EditText etPassword = new EditText(this);
        etPassword.setHint("Şifre");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
            .setTitle("PRO ERİŞİM")
            .setView(etPassword)
            .setPositiveButton("GİRİŞ", (d, w) -> {
                if ("ALİ".equals(etPassword.getText().toString())) {
                    startActivity(new Intent(this, ProSettingsActivity.class));
                } else {
                    Toast.makeText(this, "Şifre Yanlış", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }
}
