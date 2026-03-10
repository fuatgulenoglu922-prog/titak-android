package com.efe.titak;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

/**
 * BotEngine — Singleton bot durumu ve mantığı
 * Tüm servisler bu sınıf üzerinden haberleşir.
 */
public class BotEngine {

    private static BotEngine instance;

    // Bot durumu
    private boolean active = false;
    private int sessionCount = 0;
    private int totalCount = 0;
    private String status = "Durduruldu";
    private final List<String> logs = new ArrayList<>();

    // Accessibility service referansı (statik)
    public static BotAccessibilityService accessibilityService;

    // UI güncelleme callback'i (OverlayService kullanır)
    public interface BotListener {
        void onStateChanged(boolean active, int session, int total, String status);
        void onLog(String message);
    }

    private BotListener listener;

    // ─── Singleton ───────────────────────────────────────────
    public static BotEngine get() {
        if (instance == null) instance = new BotEngine();
        return instance;
    }

    private BotEngine() {}

    // ─── Getter/Setter ────────────────────────────────────────
    public boolean isActive()     { return active; }
    public int getSession()       { return sessionCount; }
    public int getTotal()         { return totalCount; }
    public String getStatus()     { return status; }
    public List<String> getLogs() { return logs; }

    public void setListener(BotListener l) { this.listener = l; }

    // ─── Kontrol ──────────────────────────────────────────────
    public void start() {
        active = true;
        status = "TikTok bekleniyor...";
        log("▶️ Bot başlatıldı");
        notifyState();
    }

    public void stop() {
        active = false;
        status = "Durduruldu";
        log("⏹️ Bot durduruldu");
        notifyState();
    }

    // ─── Jeton Toplandı ───────────────────────────────────────
    public void onCoinCollected(String source) {
        sessionCount++;
        totalCount++;
        status = "✅ Jeton toplandı! (" + sessionCount + ")";
        log("🎉 JETON TOPLANDI [" + source + "] → Toplam: " + totalCount);
        notifyState();
    }

    // ─── Durum Güncelle ───────────────────────────────────────
    public void updateStatus(String msg) {
        status = msg;
        notifyState();
    }

    // ─── Log ──────────────────────────────────────────────────
    public void log(String msg) {
        String entry = "[" + currentTime() + "] " + msg;
        logs.add(entry);
        if (logs.size() > 200) logs.remove(0);
        if (listener != null) listener.onLog(entry);
        android.util.Log.d("TT123", msg);
    }

    // ─── Sıfırla ──────────────────────────────────────────────
    public void resetStats() {
        sessionCount = 0;
        totalCount = 0;
        logs.clear();
        log("🗑️ Sıfırlandı");
        notifyState();
    }

    // ─── AI Haberleşme ─────────────────────────────────────────

    private long lastActionTime = 0;
    private int noChestCount = 0;
    private boolean isWatchingChest = false;
    private boolean hasClickedChest = false;

    public void requestTap(float x, float y) {
        if (accessibilityService != null) {
            accessibilityService.performTapGesture(x, y);
            noChestCount = 0; // reset
            isWatchingChest = false;
        } else {
            log("❌ Erişilebilirlik kapalı, tıklanamıyor!");
        }
    }

    public void forceSwipe() {
        if (accessibilityService != null) {
            accessibilityService.autoSwipe();
            noChestCount = 0;
            isWatchingChest = false;
            hasClickedChest = false;
        }
    }
    
    public void setChestClicked(boolean clicked) {
        this.hasClickedChest = clicked;
    }

    public boolean hasClickedChest() {
        return hasClickedChest;
    }
    
    public void registerChestWatching() {
        noChestCount = 0; // Sandık varken sayaç sıfırlanır, kaydırmaz.
        isWatchingChest = true;
    }

    public void recordScan(boolean foundTarget) {
        if (foundTarget) {
            noChestCount = 0;
            // updateStatus("🎯 Hedef bulundu");
        } else {
            if (isWatchingChest) {
                // Eğer sandık varsa sabırla bekle. Saatlerce bile sürebilir.
                // Sandık popup'ı açıldığında arka plan kararır ve şablon eşleşmeyebilir,
                // bu yüzden hemen kaydırmamak çok önemli.
                noChestCount++;
                updateStatus("⏳ Sandık Açılması Bekleniyor... " + (noChestCount * 300 / 1000) + "sn");
                // 300ms * 400 = 120 saniye boyunca bekle (2 dakika)
                if (noChestCount >= 400) {
                    log("⏭️ 2 Dakika Geçti, Sandık Açılmadı. Kaydırılıyor...");
                    forceSwipe();
                }
            } else {
                noChestCount++;
                updateStatus("👀 Ekran Tarandı (Hedef yok) " + noChestCount + "/50");
                // 300ms * 50 = 15 saniye hedef yoksa kaydır
                if (noChestCount >= 50) { 
                    log("⏭️ Sandık Yok. Kaydırılıyor...");
                    forceSwipe();
                }
            }
        }
    }

    private void notifyState() {
        if (listener != null) {
            listener.onStateChanged(active, sessionCount, totalCount, status);
        }
    }

    private String currentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }
}
