package com.efe.titak;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;

public class BotAccessibilityService extends AccessibilityService {

    private static final String[] TIKTOK_PACKAGES = {
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",
        "com.zhiliaoapp.musically.go"
    };

    private static final String[] TREASURE_KEYWORDS = {
        "treasure", "chest", "lucky", "hazine", "sandık"
    };

    private static final String[] CLAIM_KEYWORDS = {
        "open", "collect", "claim", "receive", "grab",
        "aç", "topla", "al", "ödülü al", "hemen al"
    };

    private static final String[] SUCCESS_KEYWORDS = {
        "kazandın", "jeton", "coin", "tebrikler", "got", "kutladın"
    };

    private static final String[] EMPTY_KEYWORDS = {
        "boş", "empty", "bitti", "better luck", "try again", "sonraki"
    };

    private long lastActionTime = 0;
    private static final long COOLDOWN_MS = 2000;
    
    // States
    private static final int STATE_SCANNING = 0;
    private static final int STATE_CLICKED_CHEST = 1;
    private static final int STATE_CLICKED_OPEN = 2;
    private int state = STATE_SCANNING;

    private Handler handler;
    private Runnable swipeRunnable;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        BotEngine.accessibilityService = this;
        BotEngine.get().log("✅ Erişilebilirlik servisi bağlandı");
        
        handler = new Handler(Looper.getMainLooper());
        swipeRunnable = () -> {
            if (BotEngine.get().isActive() && state == STATE_SCANNING) {
                BotEngine.get().log("⏭️ Sandık bulunamadı, kaydırılıyor...");
                autoSwipe();
            }
            scheduleSwipe();
        };
        scheduleSwipe();
    }

    @Override
    public void onInterrupt() {
        BotEngine.accessibilityService = null;
        BotEngine.get().log("⚠️ Erişilebilirlik servisi kesildi");
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BotEngine.accessibilityService = null;
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }

    private void scheduleSwipe() {
        if (handler != null) {
            handler.removeCallbacks(swipeRunnable);
            // Her 15 saniyede bir kaydır (eğer sandık bulunmazsa)
            handler.postDelayed(swipeRunnable, 15000);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!BotEngine.get().isActive()) {
            state = STATE_SCANNING; // reset
            return;
        }

        CharSequence pkg = event.getPackageName();
        if (pkg == null || !isTikTokPackage(pkg.toString())) return;

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {

            long now = System.currentTimeMillis();
            if (now - lastActionTime < COOLDOWN_MS) return;

            processScreen();
        }
    }

    private void processScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        BotEngine.get().updateStatus("📺 TikTok taranıyor... (" + getStateName() + ")");

        if (state == STATE_CLICKED_OPEN) {
            // Wait for success or empty message
            if (findAndHandleResult(root)) {
                root.recycle();
                return;
            }
        } 
        
        if (state == STATE_CLICKED_CHEST || state == STATE_SCANNING) {
            // Look for Claim / Open button
            AccessibilityNodeInfo claimNode = findNodeByKeywords(root, CLAIM_KEYWORDS);
            if (claimNode != null) {
                BotEngine.get().log("🎯 'Aç' butonu bulundu!");
                performNodeClick(claimNode, "claim-button");
                state = STATE_CLICKED_OPEN;
                scheduleSwipe(); // prevent swipe while waiting for result
                root.recycle();
                return;
            }
        }

        if (state == STATE_SCANNING) {
            // Look for chest
            AccessibilityNodeInfo treasureNode = findNodeByKeywords(root, TREASURE_KEYWORDS);
            if (treasureNode != null) {
                BotEngine.get().log("🎁 Sandık bulundu!");
                performNodeClick(treasureNode, "treasure-chest");
                state = STATE_CLICKED_CHEST;
                scheduleSwipe(); // reset timeout
            }
        }

        root.recycle();
    }

    private boolean findAndHandleResult(AccessibilityNodeInfo root) {
        // Success check
        AccessibilityNodeInfo successNode = findNodeByKeywords(root, SUCCESS_KEYWORDS);
        if (successNode != null) {
            BotEngine.get().onCoinCollected("success-dialog");
            state = STATE_SCANNING;
            closeDialogAndSwipe();
            return true;
        }

        // Empty check
        AccessibilityNodeInfo emptyNode = findNodeByKeywords(root, EMPTY_KEYWORDS);
        if (emptyNode != null) {
            BotEngine.get().log("❌ Sandık boş çıktı veya bitti.");
            state = STATE_SCANNING;
            closeDialogAndSwipe();
            return true;
        }
        
        return false;
    }

    private void closeDialogAndSwipe() {
        // Just swipe to move to the next stream, closing any dialog implicitly in TikTok
        handler.postDelayed(() -> {
            autoSwipe();
            scheduleSwipe();
        }, 1500);
    }

    private AccessibilityNodeInfo findNodeByKeywords(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) return null;

        String contentDesc = node.getContentDescription() != null
            ? node.getContentDescription().toString().toLowerCase() : "";
        String text = node.getText() != null
            ? node.getText().toString().toLowerCase() : "";
        
        String combined = contentDesc + " " + text;

        for (String kw : keywords) {
            if (combined.contains(kw)) {
                if (node.isClickable() && node.isVisibleToUser()) return node;
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null && parent.isClickable() && parent.isVisibleToUser()) return parent;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeByKeywords(child, keywords);
            if (result != null) return result;
            if (child != null) child.recycle();
        }

        return null;
    }

    private void performNodeClick(AccessibilityNodeInfo node, String source) {
        try {
            boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastActionTime = System.currentTimeMillis();
            if (!clicked) {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);
                performTapGesture(bounds.exactCenterX(), bounds.exactCenterY());
            }
        } catch (Exception e) {
            BotEngine.get().log("❌ Tıklama hatası: " + e.getMessage());
        }
    }

    private void performTapGesture(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
            .build();
        dispatchGesture(gesture, null, null);
    }

    private void autoSwipe() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        
        // Ekrani yukari kaydir (Swipe up)
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
        lastActionTime = System.currentTimeMillis();
        state = STATE_SCANNING; // Reset state
    }

    private boolean isTikTokPackage(String pkg) {
        for (String p : TIKTOK_PACKAGES) {
            if (pkg.equals(p)) return true;
        }
        return false;
    }

    private String getStateName() {
        switch(state) {
            case STATE_SCANNING: return "Ara";
            case STATE_CLICKED_CHEST: return "Sandık Bekleniyor";
            case STATE_CLICKED_OPEN: return "Açılıyor";
            default: return "";
        }
    }
}
