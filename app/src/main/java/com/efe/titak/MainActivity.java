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
import com.google.firebase.auth.FirebaseAuth;
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
        backgroundManager.applyBackground(this);

        tvVersion = findViewById(R.id.tv_version);
        btnMusicToggle = findViewById(R.id.btn_music_toggle);
        musicManager = MusicManager.getInstance(this);

        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (tvVersion != null) tvVersion.setText("v" + v);
        } catch (Exception e) {
            if (tvVersion != null) tvVersion.setText("v5.0");
        }

        // Setup background update checker
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest updateWork = new PeriodicWorkRequest.Builder(UpdateCheckWorker.class, 12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("UpdateCheck", androidx.work.ExistingPeriodicWorkPolicy.KEEP, updateWork);
        } catch (Exception e) {}

        setupUserSession();

        // Davet Butonu
        View btnLogin = findViewById(R.id.btn_google_login);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);

        // WhatsApp ile Davet Et Butonu
        View btnInvite = findViewById(R.id.btn_invite_friend);
        if (btnInvite != null) btnInvite.setOnClickListener(v -> shareInviteLink());

        // Pro Özellikler Butonu
        View btnPro = findViewById(R.id.btn_pro_features);
        if (btnPro != null) btnPro.setOnClickListener(v -> showProPasswordDialog());

        // Güncelleme butonu
        View btnUpdate = findViewById(R.id.btn_update_app);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                UpdateManager updateManager = new UpdateManager(this);
                updateManager.checkAndInstallUpdate();
            });
        }

        // Ayarlar Butonu
        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.theme_enter, R.anim.theme_exit);
            });
        }

        // Geri Bildirim Butonu
        View btnFeedback = findViewById(R.id.btn_feedback);
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> {
                startActivity(new Intent(this, FeedbackActivity.class));
            });
        }

        // Müzik Kontrol Butonu
        if (btnMusicToggle != null) {
            btnMusicToggle.setOnClickListener(v -> {
                musicManager.toggleMusic();
            });
        }

        // Müziği otomatik başlat
        boolean autoPlayMusic = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("auto_play_music", true);
        if (autoPlayMusic) {
            musicManager.playMusic();
        }

        // Sosyal Butonlar
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
        etUsername.setHint("Adınız");

        new AlertDialog.Builder(this)
            .setTitle("TiTak'a Hoş Geldiniz")
            .setMessage("Lütfen devam etmek için bir kullanıcı adı girin.")
            .setView(etUsername)
            .setCancelable(false)
            .setPositiveButton("Başla", (d, w) -> {
                String name = etUsername.getText().toString().trim();
                if (name.isEmpty()) name = "Kullanıcı" + new java.util.Random().nextInt(100);
                
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
        String inviteMsg = "TiTak'ta benimle konuşmaya başla! ID'm: " + myId + "\nUygulamayı indir ve bu linke tıkla: https://titak.efe.com/invite?id=" + myId;
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, inviteMsg);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");
        
        try {
            startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "WhatsApp yüklü değil.", Toast.LENGTH_SHORT).show();
            sendIntent.setPackage(null);
            startActivity(Intent.createChooser(sendIntent, "Arkadaşını Davet Et"));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null && data.getPath() != null && data.getPath().equals("/invite")) {
            String inviterId = data.getQueryParameter("id");
            if (inviterId != null && !inviterId.isEmpty()) {
                SocialManager.getInstance().sendFriendRequest(inviterId, new SocialManager.SocialCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(MainActivity.this, "Arkadaş otomatik olarak eklendi!", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, "Arkadaş eklenirken hata: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void showProPasswordDialog() {
        EditText etPassword = new EditText(this);
        etPassword.setHint("Şifre yazınız");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
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
            .setNegativeButton("Iptal", null)
            .show();
    }
}
