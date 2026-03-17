package com.efe.titak;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.efe.titak.manager.SocialManager;
import com.efe.titak.model.User;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView tvDisplayName = findViewById(R.id.tvDisplayName);
        TextView tvTitakId = findViewById(R.id.tvTitakId);

        User currentUser = SocialManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            tvDisplayName.setText(currentUser.getDisplayName());
            tvTitakId.setText("ID: " + currentUser.getTitakId());
            
            // Canlı güncelleme dinleyicisi (ID sonradan yüklenirse)
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        User updatedUser = value.toObject(User.class);
                        if (updatedUser != null) {
                            tvDisplayName.setText(updatedUser.getDisplayName());
                            tvTitakId.setText("ID: " + updatedUser.getTitakId());
                        }
                    }
                });
        }

        findViewById(R.id.btnSocial).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SocialActivity.class));
        });
    }
}
