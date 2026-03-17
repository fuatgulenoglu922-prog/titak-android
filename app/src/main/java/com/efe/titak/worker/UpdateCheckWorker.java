package com.efe.titak.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.efe.titak.MainActivity;
import com.efe.titak.R;

public class UpdateCheckWorker extends Worker {

    private static final String GITHUB_REPO = "fuatgulenoglu922-prog/titak-android";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String USER_AGENT = "TiTak-Android-Updater/3.8";

    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                Scanner s = new Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();
                
                JSONObject releaseInfo = new JSONObject(response);
                String tagName = releaseInfo.getString("tag_name");
                
                String localVersion = "1.0";
                try {
                    localVersion = getApplicationContext().getPackageManager()
                        .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
                } catch (Exception ignored) {}

                String normalizedTag = tagName;
                if (normalizedTag.startsWith("v")) {
                    normalizedTag = normalizedTag.substring(1);
                }
                
                if (!"latest".equalsIgnoreCase(tagName) && !normalizedTag.equals(localVersion)) {
                    // There's a new version!
                    showUpdateNotification(normalizedTag);
                }
            }
            conn.disconnect();
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void showUpdateNotification(String newVersion) {
        String channelId = "titak_updates";
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Güncellemeler", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Yeni uygulama sürümü bildirimleri");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Yeni Güncelleme Mevcut!")
                .setContentText("TiTak v" + newVersion + " yayınlandı. Güncellemek için tıklayın.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1001, builder.build());
    }
}
