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
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
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

    private static final long SCAN_INTERVAL_MS = 30; // 30ms for super fast reaction

    private Mat templateChest;
    private Mat templateOpen;
    private Mat templateEmpty;

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
        handler = new Handler(Looper.getMainLooper());
        startForeground(2, buildNotification());
    }

    private void loadTemplates() {
        templateChest = loadMatFromUri("tpl_chest");
        templateOpen = loadMatFromUri("tpl_open");
        templateEmpty = loadMatFromUri("tpl_empty");
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
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB); // Template matching works best on RGB/Grayscale
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
            imageReader.getSurface(), null, handler
        );

        startImagePolling();
        BotEngine.get().log("🚀 OpenCV Ekran Tarayıcı Başlatıldı");
    }

    private Runnable pollRunnable;

    private void startImagePolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!BotEngine.get().isActive()) {
                    handler.postDelayed(this, SCAN_INTERVAL_MS);
                    return;
                }

                Image image = null;
                try {
                    image = imageReader.acquireLatestImage();
                    if (image != null) {
                        processImage(image);
                    } else {
                        // Eğer boş frame gelirse (ekran değişmediyse) chest olmadığını varsaymamak lazım, 
                        // ama yine de tarama kaydedilebilir.
                        // Şimdilik pas geçiyoruz.
                    }
                } catch (Exception e) {
                    if (image != null) image.close();
                }

                handler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        };
        handler.post(pollRunnable);
    }

    private void processImage(Image image) {
        if (isScanning || templateChest == null || templateOpen == null) {
            if (templateChest == null || templateOpen == null) {
                BotEngine.get().updateStatus("⚠️ Lütfen Ana Ekranda Şablon Seçin!");
            }
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

        Bitmap screenBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        bitmap.recycle();

        Mat mScreen = new Mat();
        Utils.bitmapToMat(screenBitmap, mScreen);
        screenBitmap.recycle();
        
        Imgproc.cvtColor(mScreen, mScreen, Imgproc.COLOR_RGBA2RGB);

        boolean foundSomething = checkTemplates(mScreen);
        
        mScreen.release();
        
        isScanning = false;
    }

    private long lastChestTapTime = 0;

    private boolean checkTemplates(Mat screen) {
        boolean found = false;
        double thresholdChest = 0.55; // Sandığı daha rahat bulsun
        double thresholdOpen = 0.50;  // Aç yazısını animasyon bitmeden çok erken bulsun
        double thresholdEmpty = 0.60;

        // 1. Önce "Boş / Bitti / Tamam" şablonuna bak
        if (templateEmpty != null) {
            Point p = match(screen, templateEmpty, thresholdEmpty);
            if (p != null) {
                // ...
                BotEngine.get().updateStatus("❌ Tamam/Boş Butonu Eşleşti, Kapatılıyor");
                BotEngine.get().requestTap((float) p.x + (templateEmpty.cols() / 2f), (float) p.y + (templateEmpty.rows() / 2f));
                BotEngine.get().forceSwipe(); // Tıkladıktan sonra yayını geç
                return true;
            }
        }

        // 2. Aç butonuna bak (Eğer varsa tıkla)
        Point openPoint = match(screen, templateOpen, thresholdOpen);
        if (openPoint != null) {
            BotEngine.get().updateStatus("🎯 AÇ Butonu Eşleşti, Tıklanıyor!");
            // Hızlıca 2 kez tıklaması için erişilebilirliğe ardarda komut gönderilebilir
            // Fakat 30ms döngü hızı zaten saniyede 33 kez tıklayacaktır.
            BotEngine.get().requestTap((float) openPoint.x + (templateOpen.cols() / 2f), (float) openPoint.y + (templateOpen.rows() / 2f));
            BotEngine.get().registerChestWatching(); 
            return true;
        }

        // 3. Sandık İkonuna bak (Eğer varsa tıkla ve bekle)
        Point chestPoint = match(screen, templateChest, thresholdChest);
        if (chestPoint != null) {
            BotEngine.get().registerChestWatching(); 
            
            // Sandığın üstüne SADECE 1 KERE bas (sürekli açıp kapatmaması için)
            if (!BotEngine.get().hasClickedChest()) {
                BotEngine.get().updateStatus("🎁 Sandık İkonu Görüldü, Açılıyor...");
                BotEngine.get().requestTap((float) chestPoint.x + (templateChest.cols() / 2f), (float) chestPoint.y + (templateChest.rows() / 2f));
                BotEngine.get().setChestClicked(true);
            } else {
                BotEngine.get().updateStatus("🎁 Sandık Sayacı Bekleniyor...");
            }
            return true;
        }

        // Hiçbir şey bulunamadı
        BotEngine.get().recordScan(false);
        return false;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
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
