package com.efe.titak;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    private final Context context;
    private static final String APK_URL = "https://github.com/fuatgulenoglu922-prog/titak-android/releases/download/latest/app-debug.apk";

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkAndInstallUpdate() {
        new VersionCheckTask().execute("https://raw.githubusercontent.com/fuatgulenoglu922-prog/titak-android/main/version.txt");
    }

    private class VersionCheckTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String v = s.hasNext() ? s.next().trim() : "";
                is.close();
                return v;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String remoteVersion) {
            if (remoteVersion == null || remoteVersion.isEmpty()) {
                Toast.makeText(context, "Versiyon kontrolü başarısız!", Toast.LENGTH_SHORT).show();
                return;
            }

            remoteVersion = remoteVersion.trim();
            String localVersion = "1.0";
            try {
                localVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (Exception ignored) {}

            localVersion = localVersion.trim();

            if (remoteVersion.equals(localVersion)) {
                Toast.makeText(context, "Zaten güncel (v" + localVersion + ")", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Yeni sürüm bulundu: v" + remoteVersion + " (Mevcut: v" + localVersion + ")", Toast.LENGTH_SHORT).show();
                new DownloadTask().execute(APK_URL);
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
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                
                File apkFile = new File(context.getExternalCacheDir(), "update.apk");
                output = new FileOutputStream(apkFile);

                byte[] data = new byte[4096];
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
