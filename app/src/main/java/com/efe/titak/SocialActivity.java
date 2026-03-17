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

        // Load friend requests
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null || getSharedPreferences("titak_prefs", MODE_PRIVATE).getBoolean("is_local_user", false)) {
            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : getSharedPreferences("titak_prefs", MODE_PRIVATE).getString("local_uid", "");
            if (!uid.isEmpty()) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("friendRequests")
                        .addSnapshotListener((value, error) -> {
                            if (error != null) return;
                            if (value != null && !value.isEmpty()) {
                                Toast.makeText(this, value.size() + " yeni arkadaşlık isteği var!", Toast.LENGTH_SHORT).show();
                                // Basic auto-accept for demo purposes or user can handle it in a real adapter
                                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                    String fromUid = doc.getString("fromUid");
                                    String fromName = doc.getString("fromDisplayName");
                                    if (fromUid != null) {
                                        SocialManager.getInstance().acceptFriendRequest(fromUid, fromName, new SocialManager.SocialCallback() {
                                            @Override
                                            public void onSuccess(String message) {
                                                Toast.makeText(SocialActivity.this, fromName + " eklendi!", Toast.LENGTH_SHORT).show();
                                            }
                                            @Override
                                            public void onError(String err) {}
                                        });
                                    }
                                }
                            }
                        });
                        
                // Fetch friends to open chat
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("friends")
                        .addSnapshotListener((value, error) -> {
                            if (error != null) return;
                            if (value != null && !value.isEmpty()) {
                                // For now, just auto-open chat with the first friend if available and not already in chat
                                // In a real app, this would populate the RecyclerView rvRequests (repurposed as Friends List)
                                Toast.makeText(this, "Arkadaş listeniz güncellendi.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    } // end onCreate
}
