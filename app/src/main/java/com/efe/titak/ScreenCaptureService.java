package com.efe.titak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA";

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private int width;
    private int height;
    private int density;

    private Handler handler;
    private boolean isScanning = false;
    private TextRecognizer textRecognizer;

    private long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 1500; // 1.5 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        startForeground(2, buildNotification());
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "screen_capture", "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "screen_capture")
                .setContentTitle("titak 123 (AI)")
                .setContentText("Ekran analiz ediliyor...")
                .setSmallIcon(R.drawable.ic_coin) // Make sure ic_coin exists
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            startCapture(resultCode, resultData);
        } else if (ACTION_STOP.equals(action)) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent resultData) {
        if (mediaProjection != null) return;
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
        if (mediaProjection == null) {
            stopSelf();
            return;
        }

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);

        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        // ImageReader for screen pixels
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(), null, handler
        );

        imageReader.setOnImageAvailableListener(reader -> {
            if (!BotEngine.get().isActive()) return;
            
            long now = System.currentTimeMillis();
            if (now - lastScanTime < SCAN_INTERVAL_MS) {
                // Ignore too frequent frames to save battery
                Image image = null;
                try { image = reader.acquireLatestImage(); } catch (Exception e){}
                if (image != null) image.close();
                return;
            }

            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    lastScanTime = now;
                    processImage(image);
                }
            } catch (Exception e) {
                if (image != null) image.close();
            }
        }, handler);

        BotEngine.get().log("🚀 AI Ekran Tarayıcı Başlatıldı");
    }

    private void processImage(Image image) {
        if (isScanning) {
            image.close();
            return;
        }
        isScanning = true;

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        // Create bitmap that exactly matches screen
        Bitmap screenBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        bitmap.recycle();

        InputImage inputImage = InputImage.fromBitmap(screenBitmap, 0);
        
        textRecognizer.process(inputImage)
            .addOnSuccessListener(visionText -> {
                screenBitmap.recycle();
                parseScreenText(visionText);
                isScanning = false;
            })
            .addOnFailureListener(e -> {
                screenBitmap.recycle();
                isScanning = false;
                BotEngine.get().log("❌ OCR Hatası: " + e.getMessage());
            });
    }

    private void parseScreenText(com.google.mlkit.vision.text.Text visionText) {
        String fullText = visionText.getText().toLowerCase();
        
        // Define click targets based strictly on text coordinates
        // We look for Claim / Aç words first natively. 
        // Then we look for Treasure chest (it might have a numeric countdown or "hazine" nearby).

        boolean foundAction = false;

        for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
            for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                String lText = line.getText().toLowerCase();
                
                // 1. Success Message Detection
                if (containsSuccess(lText)) {
                    BotEngine.get().onCoinCollected("ai-vision-success");
                    return;
                }

                // 2. Empty Message Detection
                if (containsEmpty(lText)) {
                    BotEngine.get().log("❌ Kutu boş veya bitti (AI)");
                    BotEngine.get().forceSwipe(); // Request AccessibilityService to swipe
                    return;
                }

                // 3. Action Buttons (Open, Claim, Aç, Topla)
                if (containsAction(lText)) {
                    android.graphics.Rect bbox = line.getBoundingBox();
                    if (bbox != null) {
                        float cx = bbox.exactCenterX();
                        float cy = bbox.exactCenterY();
                        BotEngine.get().log("🎯 Hedef bulundut: " + lText);
                        BotEngine.get().requestTap(cx, cy);
                        foundAction = true;
                        break;
                    }
                }
            }
            if (foundAction) break;
        }

        // If nothing is found, we might want to swipe after some time
        BotEngine.get().recordScan(foundAction);
    }

    private boolean containsSuccess(String text) {
        return text.contains("kazandın") || text.contains("jeton") || text.contains("tebrikler") 
               || text.contains("got") || text.contains("kutla");
    }

    private boolean containsEmpty(String text) {
        return text.contains("boş") || text.contains("empty") || text.contains("bitti") 
               || text.contains("try again");
    }

    private boolean containsAction(String text) {
        return text.equals("open") || text.equals("aç") || text.equals("claim") || text.equals("al")
               || text.equals("topla") || text.equals("receive") || text.contains("treasure");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        BotEngine.get().log("🛑 AI Ekran Tarayıcı Durduruldu");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
