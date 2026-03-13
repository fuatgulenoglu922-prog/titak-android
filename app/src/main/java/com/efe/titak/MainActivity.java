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

    private Button btnStartOverlay;
    private CardView cardOverlay, cardAccessibility;
    private ImageView iconOverlay, iconAccessibility;
    private TextView tvOverlayStatus, tvAccessibilityStatus;
    private TextView tvVersion;
    
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartOverlay    = findViewById(R.id.btn_start_overlay);
        cardOverlay        = findViewById(R.id.card_overlay);
        cardAccessibility  = findViewById(R.id.card_accessibility);
        iconOverlay        = findViewById(R.id.icon_overlay);
        iconAccessibility  = findViewById(R.id.icon_accessibility);
        tvOverlayStatus    = findViewById(R.id.tv_overlay_status);
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status);
        tvVersion          = findViewById(R.id.tv_version);
        
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // Şifre Koruması Kontrolü
        boolean passEnabled = getSharedPreferences("bot_prefs", MODE_PRIVATE).getBoolean("password_enabled", false);
        if (passEnabled && !getIntent().getBooleanExtra("unlocked", false)) {
            startActivity(new Intent(this, LockActivity.class));
            finish();
            return;
        }

        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + v + " — OpenCV Kesin Çözüm");
        } catch (Exception e) {
            tvVersion.setText("v3.1");
        }

        // Overlay izni butonu
        cardOverlay.setOnClickListener(v -> requestOverlayPermission());

        // Erişilebilirlik butonu
        cardAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        // Resim Seçiciler
        ImageView imgChest = findViewById(R.id.img_tpl_chest);
        ImageView imgOpen = findViewById(R.id.img_tpl_open);
        ImageView imgEmpty = findViewById(R.id.img_tpl_empty);

        loadSavedImage(imgChest, "tpl_chest");
        loadSavedImage(imgOpen, "tpl_open");
        loadSavedImage(imgEmpty, "tpl_empty");

        imgChest.setOnClickListener(v -> pickImage("tpl_chest"));
        imgOpen.setOnClickListener(v -> pickImage("tpl_open"));
        imgEmpty.setOnClickListener(v -> pickImage("tpl_empty"));

        // Kayan widget başlat
        btnStartOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }
            if (!isAccessibilityEnabled()) {
                showAccessibilityDialog();
                return;
            }
            // Request Screen Recording Permission
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        });

        // Güncelleme butonu
        Button btnUpdateApp = findViewById(R.id.btn_update_app);
        btnUpdateApp.setOnClickListener(v -> {
            UpdateManager updateManager = new UpdateManager(this);
            updateManager.checkAndInstallUpdate();
        });

        // Durdur Butonu
        Button btnStopBot = findViewById(R.id.btn_stop_bot);
        btnStopBot.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Bot durduruldu.", Toast.LENGTH_SHORT).show();
        });

        // Ayarlar Butonu
        Button btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private String currentImageKey;

    private void pickImage(String key) {
        currentImageKey = key;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 2002);
    }

    private void saveImageUri(String key, Uri uri) {
        getSharedPreferences("bot_prefs", MODE_PRIVATE)
            .edit()
            .putString(key, uri.toString())
            .apply();
    }

    private void loadSavedImage(ImageView imageView, String key) {
        String uriStr = getSharedPreferences("bot_prefs", MODE_PRIVATE).getString(key, null);
        if (uriStr != null) {
            imageView.setImageURI(Uri.parse(uriStr));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                startOverlayService();
                
                Intent captureIntent = new Intent(this, ScreenCaptureService.class);
                captureIntent.setAction(ScreenCaptureService.ACTION_START);
                captureIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
                captureIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(captureIntent);
                } else {
                    startService(captureIntent);
                }
                moveTaskToBack(true);
            }
        } else if (requestCode == 2002 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                saveImageUri(currentImageKey, uri);
                ImageView iv = null;
                if ("tpl_chest".equals(currentImageKey)) iv = findViewById(R.id.img_tpl_chest);
                if ("tpl_open".equals(currentImageKey)) iv = findViewById(R.id.img_tpl_open);
                if ("tpl_empty".equals(currentImageKey)) iv = findViewById(R.id.img_tpl_empty);
                if (iv != null) iv.setImageURI(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionCards();
    }

    private void updatePermissionCards() {
        boolean overlayOk = Settings.canDrawOverlays(this);
        boolean accessOk  = isAccessibilityEnabled();

        // Overlay izni
        iconOverlay.setImageResource(overlayOk ? R.drawable.ic_check : R.drawable.ic_warning);
        tvOverlayStatus.setText(overlayOk ? "✅ İzin Verildi" : "❌ İzin Gerekli — Dokunun");
        cardOverlay.setCardBackgroundColor(getColor(overlayOk ? R.color.card_ok : R.color.card_warn));

        // Erişilebilirlik
        iconAccessibility.setImageResource(accessOk ? R.drawable.ic_check : R.drawable.ic_warning);
        tvAccessibilityStatus.setText(accessOk ? "✅ Aktif" : "❌ Kapalı — Dokunun");
        cardAccessibility.setCardBackgroundColor(getColor(accessOk ? R.color.card_ok : R.color.card_warn));

        // Başlat butonu
        boolean ready = overlayOk && accessOk;
        btnStartOverlay.setEnabled(true);
        btnStartOverlay.setText(ready ? "🚀 Botu Başlat" : "İzinleri Ver → Botu Başlat");
        btnStartOverlay.setAlpha(ready ? 1.0f : 0.7f);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void openAccessibilitySettings() {
        showAccessibilityDialog();
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Erişilebilirlik Aktivasyonu")
            .setMessage(
                "1. Ayarlar → Erişilebilirlik'e gidin\n" +
                "2. İndirilen Uygulamalar'ı açın\n" +
                "3. \"(efe)\" uygulamasını bulun\n" +
                "4. Açın ve İzin Verin\n\n" +
                "Bu izin TikTok ekranındaki sandıkları otomatik tıklamak için gereklidir."
            )
            .setPositiveButton("Ayarlara Git", (d, w) -> {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private boolean isAccessibilityEnabled() {
        try {
            String service = getPackageName() + "/" + BotAccessibilityService.class.getCanonicalName();
            int enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            );
            if (enabled != 1) return false;
            String settingValue = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return settingValue != null && settingValue.contains(service);
        } catch (Exception e) {
            return false;
        }
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
