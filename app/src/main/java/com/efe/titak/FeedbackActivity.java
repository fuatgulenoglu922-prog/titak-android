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
import com.efe.titak.ai.AIConnector;
import com.efe.titak.model.APIKey;
import com.efe.titak.model.Message;

import java.util.ArrayList;
import java.util.List;

public class FeedbackActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend, btnBack;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private APIManager apiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);

        apiManager = new APIManager(this);
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        APIKey active = apiManager.getActiveAPIKey();
        String welcome = active != null
                ? "Merhaba! Yapay zeka destekli sohbet aktif (otomatik tespit: " + AIConnector.detectProvider(active.getBaseUrl()) + "). Sorunuzu yazın."
                : "Merhaba! Geri bildirim veya soru yazabilirsiniz. Yapay zeka yanıtı için Ayarlar'dan API ekleyin.";
        addMessage(new Message(welcome, true));

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            addMessage(new Message(text, false));
            etMessage.setText("");

            APIKey key = apiManager.getActiveAPIKey();
            if (key == null) {
                addMessage(new Message("API anahtarı ekleyip birini seçin (Ana menü → API Bağlantıcı).", true));
                return;
            }

            btnSend.setEnabled(false);
            buildHistoryAndSend(key, text);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void buildHistoryAndSend(APIKey key, String userText) {
        List<AIConnector.ChatMessage> history = new ArrayList<>();
        int maxHistory = 10;
        int start = Math.max(0, messageList.size() - maxHistory - 1);
        for (int i = start; i < messageList.size() - 1; i++) {
            Message m = messageList.get(i);
            history.add(new AIConnector.ChatMessage(!m.isFromBot(), m.getText()));
        }

        new Thread(() -> {
            try {
                AIConnector connector = new AIConnector(key);
                String reply = connector.sendMessage(userText, history);
                runOnUiThread(() -> {
                    addMessage(new Message(reply != null && !reply.isEmpty() ? reply : "Yanıt alınamadı.", true));
                    btnSend.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addMessage(new Message("Hata: " + e.getMessage(), true));
                    btnSend.setEnabled(true);
                });
            }
        }).start();
    }

    private void addMessage(Message message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
    }
}