package com.efe.titak;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProSettingsActivity extends AppCompatActivity {

    private final String[] effects = {
        "Normal", "Yaşlı Adam", "Çocuk", "Domuz", "Hulk", 
        "Sincap (Chipmunk)", "Arı / Tiz", "Derin / Kalın", 
        "Elektronik", "Eteryel (Yankılı)", "Pop Star"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_settings);

        LinearLayout container = findViewById(R.id.container_effects);
        int currentEffect = getSharedPreferences("pro_prefs", MODE_PRIVATE).getInt("voice_effect", 0);

        for (int i = 0; i < effects.length; i++) {
            Button btn = new Button(this);
            btn.setText(effects[i] + (currentEffect == i ? " (Seçili)" : ""));
            btn.setAllCaps(false);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(currentEffect == i ? R.color.accent_green : R.color.panel_cyber)
            ));
            
            final int index = i;
            btn.setOnClickListener(v -> {
                getSharedPreferences("pro_prefs", MODE_PRIVATE).edit().putInt("voice_effect", index).apply();
                Toast.makeText(this, effects[index] + " seçildi.", Toast.LENGTH_SHORT).show();
                finish();
            });
            container.addView(btn);
        }

        findViewById(R.id.btn_save_effect).setOnClickListener(v -> finish());
    }
}
