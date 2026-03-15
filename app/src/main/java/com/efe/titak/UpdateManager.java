package com.efe.titak;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    private final Context context;

    // GitHub repo — kendi reponuza göre değiştirin
    private static final String GITHUB_REPO = "fuatgulenoglu922-prog/titak-android";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String USER_AGENT = "TiTak-Android-Updater/2.8";

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkAndInstallUpdate() {
        new GitHubReleaseTask().execute(GITHUB_API_URL);
    }

    private class GitHubReleaseTask extends AsyncTask<String, Void, JSONObject> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Güncelleme Kontrolü");
            progressDialog.setMessage("Son sürüm kontrol ediliyor...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... urls) {
            HttpURLConnection conn = null;
            try {
                String urlStr = urls[0];
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("Cache-Control", "no-cache");
                
                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) return null;
                
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();
                
                return new JSONObject(response);
            } catch (Exception e) {
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(JSONObject releaseInfo) {
            progressDialog.dismiss();
            
            if (releaseInfo == null) {
                Toast.makeText(context, "Bağlantı hatası. İnterneti kontrol edin.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                String tagName = releaseInfo.getString("tag_name");
                String body = releaseInfo.optString("body", "");
                JSONArray assets = releaseInfo.optJSONArray("assets");
                
                String downloadUrl = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        if (name.endsWith(".apk") && name.contains("debug")) {
                            downloadUrl = asset.optString("browser_download_url");
                            break;
                        }
                    }
                }
                
                if (downloadUrl == null) {
                    Toast.makeText(context, "APK dosyası bulunamadı.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String localVersion = "1.0";
                try {
                    localVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
                } catch (Exception ignored) {}

                // tag_name "latest" ise sürüm karşılaştırması yapma, doğrudan indir
                boolean isLatestTag = "latest".equalsIgnoreCase(tagName);
                if (!isLatestTag && tagName.equals(localVersion)) {
                    Toast.makeText(context, "Zaten güncel (v" + localVersion + ")", Toast.LENGTH_LONG).show();
                } else {
                    showChangelogDialog(isLatestTag ? localVersion + " → En son sürüm" : tagName, body, downloadUrl);
                }
            } catch (Exception e) {
                Toast.makeText(context, "Güncelleme hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showChangelogDialog(String version, String changelog, final String downloadUrl) {
        new AlertDialog.Builder(context)
            .setTitle("Yeni Güncelleme: v" + version)
            .setMessage(changelog.isEmpty() ? "Yeni sürüm hazır!" : changelog)
            .setPositiveButton("İndir ve Kur", (d, w) -> {
                new DownloadTask().execute(downloadUrl);
            })
            .setNegativeButton("İptal", null)
            .show();
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
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.connect();

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
