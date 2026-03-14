package com.efe.titak;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.efe.titak.adapter.MessageAdapter;
import com.efe.titak.model.Message;

import java.util.ArrayList;
import java.util.List;

public class FeedbackActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend, btnBack;
    private MessageAdapter adapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Örnek mesaj
        addMessage(new Message("Merhaba! Uygulama hakkında geri bildiriminizi bekliyoruz.", true));

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessage(new Message(text, false));
                etMessage.setText("");
                
                // Simüle edilmiş yanıt
                new android.os.Handler().postDelayed(() -> {
                    addMessage(new Message("Teşekkürler! Geri bildiriminiz için teşekkür ederiz.", true));
                }, 1000);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void addMessage(Message message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
    }
}