package com.efe.titak;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    private final Context context;

    // GitHub repo — kendi reponuza göre değiştirin (örn: "kullanici/titak-android")
    private static final String GITHUB_REPO = "fuatgulenoglu922-prog/titak-android";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/" + GITHUB_REPO + "/main/version.txt";
    private static final String APK_URL_LATEST = "https://github.com/" + GITHUB_REPO + "/releases/download/latest/app-debug.apk";
    private static final String USER_AGENT = "TiTak-Android-Updater/1.0";

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkAndInstallUpdate() {
        new VersionCheckTask().execute(VERSION_URL);
    }

    private class VersionCheckTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            try {
                String urlStr = urls[0] + "?t=" + System.currentTimeMillis();
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setUseCaches(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                conn.setRequestProperty("Pragma", "no-cache");
                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) return null;
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String v = s.hasNext() ? s.next().trim() : "";
                is.close();
                return v;
            } catch (Exception e) {
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String remoteVersion) {
            if (remoteVersion == null || remoteVersion.isEmpty()) {
                Toast.makeText(context, "Bağlantı hatası. İnterneti kontrol edin veya daha sonra tekrar deneyin.", Toast.LENGTH_LONG).show();
                return;
            }

            remoteVersion = remoteVersion.trim();
            String localVersion = "1.0";
            try {
                localVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (Exception ignored) {}
            localVersion = localVersion != null ? localVersion.trim() : "1.0";

            if (remoteVersion.equals(localVersion)) {
                Toast.makeText(context, "Zaten güncel (v" + localVersion + ")", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Yeni sürüm: v" + remoteVersion + " indiriliyor...", Toast.LENGTH_SHORT).show();
                new DownloadTask().execute(APK_URL_LATEST);
            }
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, File> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Güncelleme İndiriliyor");
            progressDialog.setMessage("Lütfen bekleyin...");
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected File doInBackground(String... sUrl) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            try {
                String urlStr = sUrl[0];
                // GitHub 302 yönlendirmesini takip et (en fazla 5 redirect)
                for (int redirect = 0; redirect < 5; redirect++) {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    connection.setInstanceFollowRedirects(false);
                    connection.connect();

                    int code = connection.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        break;
                    }
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307) {
                        String location = connection.getHeaderField("Location");
                        connection.disconnect();
                        connection = null;
                        if (location == null || location.isEmpty()) return null;
                        urlStr = location.startsWith("http") ? location : new URL(new URL(urlStr), location).toString();
                        continue;
                    }
                    return null;
                }

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                File apkFile = new File(context.getExternalCacheDir(), "update.apk");
                output = new FileOutputStream(apkFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
                return apkFile;
            } catch (Exception e) {
                return null;
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (Exception ignored) {}
                if (connection != null) connection.disconnect();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            progressDialog.dismiss();
            if (file != null) {
                installApk(file);
            } else {
                Toast.makeText(context, "İndirme hatası!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            apkUri = Uri.fromFile(file);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
