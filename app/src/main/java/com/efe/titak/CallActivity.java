package com.efe.titak;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.ChannelMediaOptions;

public class CallActivity extends AppCompatActivity {

    private final String appId = "44440000aaaa"; // Buraya geçerli bir App ID girilmeli
    private String channelName = "titak_radio_default";
    private RtcEngine mRtcEngine;
    
    private TextView tvCallStatus;
    private Button btnPushToTalk;
    private MediaPlayer beepStartPlayer, beepEndPlayer;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = { Manifest.permission.RECORD_AUDIO };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                tvCallStatus.setText("TELSİZ AKTİF");
                Toast.makeText(CallActivity.this, "Telsiz Ağına Bağlanıldı", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(CallActivity.this, "Bir birim ağa katıldı", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> Toast.makeText(CallActivity.this, "Bir birim ağdan ayrıldı", Toast.LENGTH_SHORT).show());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        tvCallStatus = findViewById(R.id.tvCallStatus);
        btnPushToTalk = findViewById(R.id.btnPushToTalk);
        
        // Ses efektlerini hazırla (Telsiz bip sesleri)
        beepStartPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        beepEndPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

        if (checkSelfPermission()) {
            initAgoraEngineAndJoinChannel();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        setupPushToTalk();

        findViewById(R.id.btnEndCall).setOnClickListener(v -> {
            leaveChannel();
            finish();
        });
        
        String targetName = getIntent().getStringExtra("targetName");
        if (targetName != null) {
            ((TextView)findViewById(R.id.tvTargetName)).setText(targetName);
            channelName = "radio_" + targetName.toLowerCase().replaceAll("\\s+", "");
        }
    }

    private void setupPushToTalk() {
        btnPushToTalk.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTalking();
                    v.performClick();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopTalking();
                    return true;
            }
            return false;
        });
    }

    private void startTalking() {
        if (mRtcEngine != null) {
            mRtcEngine.muteLocalAudioStream(false);
            btnPushToTalk.setText("KONUŞUYORSUN...");
            if (beepStartPlayer != null) beepStartPlayer.start();
        }
    }

    private void stopTalking() {
        if (mRtcEngine != null) {
            mRtcEngine.muteLocalAudioStream(true);
            btnPushToTalk.setText("BAS VE KONUŞ");
            if (beepEndPlayer != null) beepEndPlayer.start();
        }
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED;
    }

    private void initAgoraEngineAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
            
            // Agora 4.x için uyumlu ayarlar
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD, Constants.AUDIO_SCENARIO_DEFAULT);
            
            mRtcEngine.muteLocalAudioStream(true);

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.autoSubscribeAudio = true;
            options.publishMicrophoneTrack = true;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

            mRtcEngine.joinChannel(null, channelName, 0, options);
            
        } catch (Exception e) {
            Toast.makeText(this, "Telsiz başlatılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        RtcEngine.destroy();
        if (beepStartPlayer != null) beepStartPlayer.release();
        if (beepEndPlayer != null) beepEndPlayer.release();
    }
}
