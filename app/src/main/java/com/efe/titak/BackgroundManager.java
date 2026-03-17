package com.efe.titak;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class BackgroundManager {
    private static final String PREFS_NAME = "background_prefs";
    private static final String KEY_BACKGROUND_PATH = "background_path";
    private static final String KEY_USE_ONLINE = "use_online_bg";

    private final Context context;

    public BackgroundManager(Context context) {
        this.context = context;
    }

    public void pickFromGallery(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    public void downloadRandomFromInternet(Activity activity, int requestCode) {
        new DownloadRandomImageTask(activity, requestCode).execute();
    }

    public void setBackgroundFromUri(Activity activity, Uri imageUri) {
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            String path = saveBitmapToInternalStorage(bitmap);
            if (path != null) {
                saveBackgroundPath(path);
                applyBackground(activity);
                Toast.makeText(context, "Arka plan değiştirildi!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Resim yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void applyBackground(Activity activity) {
        String path = getBackgroundPath();
        if (path != null && new File(path).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), bitmap);
                activity.getWindow().setBackgroundDrawable(drawable);
            }
        }
    }

    public void applyBackgroundToView(View view) {
        String path = getBackgroundPath();
        if (path != null && new File(path).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(view.getContext().getResources(), bitmap);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    view.setBackground(drawable);
                } else {
                    view.setBackgroundDrawable(drawable);
                }
            }
        }
    }

    public void clearBackground() {
        String path = getBackgroundPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_BACKGROUND_PATH)
                .apply();
    }

    public boolean hasBackground() {
        String path = getBackgroundPath();
        return path != null && new File(path).exists();
    }

    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        try {
            File file = new File(context.getFilesDir(), "background.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private void saveBackgroundPath(String path) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BACKGROUND_PATH, path)
                .apply();
    }

    public String getBackgroundPath() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_BACKGROUND_PATH, null);
    }

    private class DownloadRandomImageTask extends AsyncTask<Void, Void, Bitmap> {
        private final Activity activity;
        private final int requestCode;
        private android.app.ProgressDialog progressDialog;

        public DownloadRandomImageTask(Activity activity, int requestCode) {
            this.activity = activity;
            this.requestCode = requestCode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new android.app.ProgressDialog(activity);
            progressDialog.setTitle("Resim İndiriliyor");
            progressDialog.setMessage("Rastgele arka plan indiriliyor...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                Random random = new Random();
                int width = 1080 + random.nextInt(500);
                int height = 1920 + random.nextInt(500);
                int seed = random.nextInt(1000);

                String urlStr = "https://picsum.photos/seed/" + seed + "/" + width + "/" + height;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    return bitmap;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            progressDialog.dismiss();
            if (bitmap != null) {
                String path = saveBitmapToInternalStorage(bitmap);
                if (path != null) {
                    saveBackgroundPath(path);
                    applyBackground(activity);
                    Toast.makeText(context, "Rastgele arka plan ayarlandı!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Resim indirilemedi. İnternet bağlantınızı kontrol edin.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
