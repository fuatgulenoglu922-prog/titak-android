package com.efe.titak;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.efe.titak.manager.SocialManager;

public class SocialActivity extends AppCompatActivity {

    private EditText etSearchId;
    private RecyclerView rvRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        etSearchId = findViewById(R.id.etSearchId);
        rvRequests = findViewById(R.id.rvRequests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String targetId = etSearchId.getText().toString().trim();
            if (targetId.length() != 4) {
                Toast.makeText(SocialActivity.this, "Lütfen 4 haneli bir ID girin.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!targetId.isEmpty()) {
                SocialManager.getInstance().sendFriendRequest(targetId, new SocialManager.SocialCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(SocialActivity.this, message, Toast.LENGTH_SHORT).show();
                        etSearchId.setText("");
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SocialActivity.this, "Hata: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // TODO: Load friend requests from Firestore and setup adapter
    }
}
