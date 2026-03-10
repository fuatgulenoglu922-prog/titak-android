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

        tvVersion.setText("v2.0 — AI Ekran Botu");

        // Overlay izni butonu
        cardOverlay.setOnClickListener(v -> requestOverlayPermission());

        // Erişilebilirlik butonu
        cardAccessibility.setOnClickListener(v -> openAccessibilitySettings());

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // Permission granted, start services
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
                
                // Ana ekranı gizle (arka plana geç)
                moveTaskToBack(true);
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
