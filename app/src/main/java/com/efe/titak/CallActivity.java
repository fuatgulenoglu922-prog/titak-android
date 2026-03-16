package com.efe.titak;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
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

public class CallActivity extends AppCompatActivity {

    private final String appId = "YOUR_AGORA_APP_ID"; // Placeholder
    private String channelName = "titak_test";
    private String token = null;

    private RtcEngine mRtcEngine;
    private TextView tvCallStatus;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> tvCallStatus.setText("Bağlandı"));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                tvCallStatus.setText("Kullanıcı ayrıldı");
                finish();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        tvCallStatus = findViewById(R.id.tvCallStatus);
        
        if (checkSelfPermission()) {
            initAgoraEngineAndJoinChannel();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        findViewById(R.id.fabDecline).setOnClickListener(v -> {
            leaveChannel();
            finish();
        });

        findViewById(R.id.fabAccept).setOnClickListener(v -> {
            // Logic for answering
        });
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
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        mRtcEngine.joinChannel(token, channelName, "", 0);
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
    }
}
