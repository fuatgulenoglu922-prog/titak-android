package com.efe.titak;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.efe.titak.manager.SocialManager;

public class ChatActivity extends AppCompatActivity {

    private String targetUid;
    private String targetName;
    private EditText etMessage;
    private RecyclerView rvMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        targetUid = getIntent().getStringExtra("targetUid");
        targetName = getIntent().getStringExtra("targetName");

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(targetName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etMessage = findViewById(R.id.etMessage);
        rvMessages = findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        ImageView btnSend = findViewById(R.id.btnSend);
        ImageView btnCall = findViewById(R.id.btnCall);

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                SocialManager.getInstance().sendMessage(targetUid, text, new SocialManager.SocialCallback() {
                    @Override
                    public void onSuccess(String message) {
                        etMessage.setText("");
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ChatActivity.this, "Hata: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Telsiz Bağlantısını Başlat
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("targetUid", targetUid);
            intent.putExtra("targetName", targetName);
            startActivity(intent);
        });

        Toast.makeText(this, "Telsiz ve Mesajlaşma Aktif", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
