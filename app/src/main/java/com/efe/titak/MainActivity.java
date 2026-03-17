package com.efe.titak;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.projection.MediaProjectionManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.efe.titak.manager.SocialManager;

public class MainActivity extends AppCompatActivity {
    private TextView tvVersion;
    private Button btnMusicToggle;
    private MusicManager musicManager;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvVersion = findViewById(R.id.tv_version);
        btnMusicToggle = findViewById(R.id.btn_music_toggle);
        musicManager = MusicManager.getInstance(this);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + v);
        } catch (Exception e) {
            tvVersion.setText("v3.4");
        }

        // Google Login Butonu
        findViewById(R.id.btn_google_login).setOnClickListener(v -> signIn());

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

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Giris Basarisiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String googlePlayId = "GP_" + FirebaseAuth.getInstance().getCurrentUser().getUid().substring(0, 5);
                        SocialManager.getInstance().syncUserProfile(
                                googlePlayId,
                                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                                null
                        );
                        Toast.makeText(this, "Giris Basarili!", Toast.LENGTH_SHORT).show();
                        recreate();
                    } else {
                        Toast.makeText(this, "Kimlik Dogrulama Hatasi.", Toast.LENGTH_SHORT).show();
                    }
                });
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
