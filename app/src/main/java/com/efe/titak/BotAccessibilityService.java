package com.efe.titak;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class BotAccessibilityService extends AccessibilityService {

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        BotEngine.accessibilityService = this;
        BotEngine.get().log("✅ Erişilebilirlik (Tıklayıcı) bağlandı");
    }

    @Override
    public void onInterrupt() {
        BotEngine.accessibilityService = null;
        BotEngine.get().log("⚠️ Erişilebilirlik servisi kesildi");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BotEngine.accessibilityService = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Artık ekrandaki yazıları (View Hierarchy) buradan okumuyoruz.
        // AI Vision servisi Ekran Görüntüsü ile pikselleri analiz edecek.
        // Bu servis SADECE tıklama ve kaydırma işlemleri için kullanılacak.
    }

    /**
     * AI tarafından tespit edilen piksellere tıklar.
     */
    public void performTapGesture(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 1))
            .build();
        dispatchGesture(gesture, null, null);
    }

    /**
     * AI 15 saniye boyunca sandık bulamazsa veya sandık bittikten sonra kaydırır.
     */
    public void autoSwipe() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float startX = metrics.widthPixels / 2f;
        float startY = metrics.heightPixels * 0.8f;
        float endY = metrics.heightPixels * 0.2f;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(startX, endY);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 300))
            .build();
        
        dispatchGesture(gesture, null, null);
    }
}
