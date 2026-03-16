package com.efe.titak;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class MusicManager {
    private static MusicManager instance;
    private MediaPlayer mediaPlayer;
    private Context context;
    private boolean isPlaying = false;
    private static final String TAG = "MusicManager";

    private MusicManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized MusicManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicManager(context);
        }
        return instance;
    }

    public void playMusic() {
        try {
            if (mediaPlayer == null) {
                // R.raw.background_music - müzik dosyanızı raw klasörüne koyun
                mediaPlayer = MediaPlayer.create(context, R.raw.background_music);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(0.5f, 0.5f);
                }
            }
            
            if (mediaPlayer != null && !isPlaying) {
                mediaPlayer.start();
                isPlaying = true;
                Log.d(TAG, "Müzik başlatıldı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Müzik çalma hatası: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            Log.d(TAG, "Müzik duraklatıldı");
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            Log.d(TAG, "Müzik durduruldu");
        }
    }

    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void toggleMusic() {
        if (isPlaying) {
            pauseMusic();
        } else {
            playMusic();
        }
    }
}
