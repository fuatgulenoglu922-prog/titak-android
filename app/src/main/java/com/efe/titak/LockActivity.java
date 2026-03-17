package com.efe.titak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        String savedPass = getSharedPreferences("bot_prefs", MODE_PRIVATE).getString("app_password", "ALİ");

        EditText etInput = findViewById(R.id.et_lock_password);
        Button btnUnlock = findViewById(R.id.btn_unlock);
        
        // Yüz tanıma butonu ve kamera görünümü artık kullanılmıyor
        // View btnFaceUnlock = findViewById(R.id.btn_face_unlock);
        // if (btnFaceUnlock != null) btnFaceUnlock.setVisibility(View.GONE);
        // View cameraView = findViewById(R.id.camera_view);
        // if (cameraView != null) cameraView.setVisibility(View.GONE);

        btnUnlock.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (input.equals(savedPass)) {
                unlockApp();
            } else {
                Toast.makeText(this, "Hatalı Şifre!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unlockApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("unlocked", true);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.theme_enter_reverse, R.anim.theme_exit_reverse);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
