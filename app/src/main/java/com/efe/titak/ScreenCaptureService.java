package com.efe.titak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private Handler scanningHandler;
    private Handler uiHandler;
    private HandlerThread scanningThread;

    private static final long SCAN_INTERVAL_MS = 10; // 10ms for very fast reaction (~100 FPS)
    private static final long OPEN_BUTTON_SCAN_INTERVAL_MS = 5; // 5ms for ultra-fast open button detection

    private Mat templateChest;
    private Mat templateOpen;
    private Mat templateEmpty;

    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private AtomicBoolean isOpenButtonVisible = new AtomicBoolean(false);
    private long lastOpenButtonDetectionTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        
        if (!OpenCVLoader.initDebug()) {
            BotEngine.get().log("❌ OpenCV başlatılamadı!");
        } else {
            BotEngine.get().log("✅ OpenCV başarıyla yüklendi.");
        }

        loadTemplates();

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // Create dedicated thread for scanning
        scanningThread = new HandlerThread("ScreenScanningThread");
        scanningThread.start();
        scanningHandler = new Handler(scanningThread.getLooper());
        uiHandler = new Handler(Looper.getMainLooper());

        startForeground(2, buildNotification());
    }

    private void loadTemplates() {
        templateChest = loadMatFromUri("tpl_chest");
        templateOpen = loadMatFromUri("tpl_open");
        templateEmpty = loadMatFromUri("tpl_empty");
        
        if (templateChest != null) {
            BotEngine.get().log("✅ Sandık şablonu yüklendi: " + templateChest.cols() + "x" + templateChest.rows());
        }
        if (templateOpen != null) {
            BotEngine.get().log("✅ Aç butonu şablonu yüklendi: " + templateOpen.cols() + "x" + templateOpen.rows());
        }
    }

    private Mat loadMatFromUri(String key) {
        try {
            String uriStr = getSharedPreferences("bot_prefs", MODE_PRIVATE).getString(key, null);
            if (uriStr == null) return null;
            
            Uri uri = Uri.parse(uriStr);
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap != null) {
                Mat mat = new Mat();
                Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
                return mat;
            }
        } catch (Exception e) {
            BotEngine.get().log("⚠️ Şablon Yükleme Hatası: " + key);
        }
        return null;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "screen_capture", "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "screen_capture")
                .setContentTitle("titak 123 (OpenCV)")
                .setContentText("Ekran analiz ediliyor...")
                .setSmallIcon(R.drawable.ic_coin)
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

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(), null, scanningHandler
        );

        startImagePolling();
        BotEngine.get().log("🚀 OpenCV Ekran Tarayıcı Başlatıldı (Ultra Hızlı Mod)");
    }

    private Runnable pollRunnable;

    private void startImagePolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!BotEngine.get().isActive()) {
                    scanningHandler.postDelayed(this, SCAN_INTERVAL_MS);
                    return;
                }

                Image image = null;
                try {
                    image = imageReader.acquireLatestImage();
                    if (image != null) {
                        processImageAsync(image);
                    }
                } catch (Exception e) {
                    if (image != null) image.close();
                }

                scanningHandler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        };
        scanningHandler.post(pollRunnable);
    }

    private void processImageAsync(Image image) {
        if (isProcessing.get()) {
            image.close();
            return;
        }

        isProcessing.set(true);

        // Create a copy of the image data in a background thread
        scanningHandler.post(() -> {
            try {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                image.close();

                Bitmap screenBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                bitmap.recycle();

                Mat mScreen = new Mat();
                Utils.bitmapToMat(screenBitmap, mScreen);
                screenBitmap.recycle();
                
                Imgproc.cvtColor(mScreen, mScreen, Imgproc.COLOR_RGBA2RGB);

                checkTemplates(mScreen);
                
                mScreen.release();
            } catch (Exception e) {
                BotEngine.get().log("⚠️ Görüntü işleme hatası: " + e.getMessage());
            } finally {
                isProcessing.set(false);
            }
        });
    }

    private long lastChestTapTime = 0;
    private static final long CHEST_TAP_COOLDOWN = 2000; // 2 seconds cooldown between chest taps

    private void checkTemplates(Mat screen) {
        long currentTime = System.currentTimeMillis();

        // 1. Check for Empty/Complete button first (highest priority)
        if (templateEmpty != null) {
            Point p = matchInRegion(screen, templateEmpty, 0.60);
            if (p != null) {
                BotEngine.get().updateStatus("❌ Tamam/Boş Butonu Eşleşti, Kapatılıyor");
                BotEngine.get().requestTap((float) p.x + (templateEmpty.cols() / 2f), (float) p.y + (templateEmpty.rows() / 2f));
                BotEngine.get().forceSwipe();
                isOpenButtonVisible.set(false);
                BotEngine.get().setChestClicked(false); // Reset chest state
                return;
            }
        }

        // 2. Check for Open button (high frequency scan)
        if (templateOpen != null) {
            // Optimize: Only scan the bottom half of the screen where buttons usually appear
            int regionHeight = height / 2;
            Mat bottomRegion = new Mat(screen, new Rect(0, height - regionHeight, width, regionHeight));
            
            Point openPoint = match(bottomRegion, templateOpen, 0.50);
            bottomRegion.release();
            
            if (openPoint != null) {
                // Convert relative coordinates back to full screen
                openPoint.y += (height - regionHeight);
                
                isOpenButtonVisible.set(true);
                lastOpenButtonDetectionTime = currentTime;
                
                BotEngine.get().updateStatus("🎯 AÇ Butonu Eşleşti, Tıklanıyor!");
                BotEngine.get().requestTap((float) openPoint.x + (templateOpen.cols() / 2f), (float) openPoint.y + (templateOpen.rows() / 2f));
                BotEngine.get().registerChestWatching();
                
                // Ultra-fast retry for open button
                scanningHandler.post(() -> {
                    // Immediately scan again for open button
                    Mat screen2 = null; // Would need to pass this, simplified for now
                });
                return;
            } else {
                isOpenButtonVisible.set(false);
            }
        }

        // 3. Check for Chest icon
        if (templateChest != null && currentTime - lastChestTapTime > CHEST_TAP_COOLDOWN) {
            Point chestPoint = match(screen, templateChest, 0.55);
            if (chestPoint != null) {
                BotEngine.get().registerChestWatching();
                
                // Only tap chest if we haven't recently
                if (!BotEngine.get().hasClickedChest()) {
                    BotEngine.get().updateStatus("🎁 Sandık İkonu Görüldü, Açılıyor...");
                    BotEngine.get().requestTap((float) chestPoint.x + (templateChest.cols() / 2f), (float) chestPoint.y + (templateChest.rows() / 2f));
                    BotEngine.get().setChestClicked(true);
                    lastChestTapTime = currentTime;
                } else {
                    BotEngine.get().updateStatus("🎁 Sandık Sayacı Bekleniyor...");
                }
                return;
            }
        }

        // Hiçbir şey bulunamadı
        BotEngine.get().recordScan(false);
    }

    private Point match(Mat screen, Mat template, double threshold) {
        if (template == null || screen.empty() || template.empty() || screen.cols() < template.cols() || screen.rows() < template.rows()) {
            return null;
        }

        Mat result = new Mat();
        Imgproc.matchTemplate(screen, template, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        result.release();

        if (mmr.maxVal >= threshold) {
            return mmr.maxLoc;
        }
        return null;
    }

    private Point matchInRegion(Mat screen, Mat template, double threshold) {
        if (template == null || screen.empty() || template.empty()) {
            return null;
        }

        // Define region of interest (center of screen for empty button)
        int startX = width / 4;
        int startY = height / 4;
        int regionWidth = width / 2;
        int regionHeight = height / 2;

        if (screen.cols() < startX + regionWidth || screen.rows() < startY + regionHeight) {
            return null;
        }

        Mat region = new Mat(screen, new Rect(startX, startY, regionWidth, regionHeight));
        Point p = match(region, template, threshold);
        region.release();

        if (p != null) {
            p.x += startX;
            p.y += startY;
        }

        return p;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanningHandler != null && pollRunnable != null) {
            scanningHandler.removeCallbacks(pollRunnable);
        }
        if (scanningThread != null) {
            scanningThread.quitSafely();
        }
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        if (templateChest != null) templateChest.release();
        if (templateOpen != null) templateOpen.release();
        if (templateEmpty != null) templateEmpty.release();
        
        BotEngine.get().log("🛑 OpenCV Ekran Tarayıcı Durduruldu");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}