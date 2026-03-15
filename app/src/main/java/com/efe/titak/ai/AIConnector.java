package com.efe.titak.ai;

import android.util.Log;

import com.efe.titak.model.APIKey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Herhangi bir yapay zeka API'sine bağlanır.
 * Base URL'den sağlayıcıyı otomatik tespit eder (OpenAI, Anthropic, Google, uyumlu).
 */
public class AIConnector {

    private static final String TAG = "AIConnector";
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;

    private final APIKey apiKey;

    public AIConnector(APIKey apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Base URL'den sağlayıcıyı otomatik tespit eder.
     */
    public static AIProvider detectProvider(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return AIProvider.OPENAI;
        String lower = baseUrl.toLowerCase();
        if (lower.contains("anthropic.com")) return AIProvider.ANTHROPIC;
        if (lower.contains("generativelanguage.googleapis.com") || lower.contains("gemini")) return AIProvider.GOOGLE;
        if (lower.contains("openai.com")) return AIProvider.OPENAI;
        return AIProvider.OPENAI_COMPATIBLE;
    }

    /**
     * Tek mesaj gönderir, yanıt metnini döndürür. Hata olursa null veya hata mesajı.
     */
    public String sendMessage(String userMessage, List<ChatMessage> history) {
        AIProvider provider = detectProvider(apiKey.getBaseUrl());
        try {
            switch (provider) {
                case ANTHROPIC:
                    return callAnthropic(userMessage, history);
                case GOOGLE:
                    return callGoogle(userMessage, history);
                case OPENAI:
                case OPENAI_COMPATIBLE:
                default:
                    return callOpenAICompatible(userMessage, history);
            }
        } catch (Exception e) {
            Log.e(TAG, "AI çağrı hatası: " + e.getMessage());
            return "Hata: " + e.getMessage();
        }
    }

    private String callOpenAICompatible(String userMessage, List<ChatMessage> history) throws Exception {
        String base = apiKey.getBaseUrl().trim();
        if (!base.endsWith("/")) base += "/";
        String urlStr = base.contains("/v1") ? (base + "chat/completions") : (base + "v1/chat/completions");

        JSONObject body = new JSONObject();
        body.put("model", apiKey.getModel() != null && !apiKey.getModel().isEmpty() ? apiKey.getModel() : "gpt-3.5-turbo");
        JSONArray messages = new JSONArray();
        if (history != null) {
            for (ChatMessage m : history) {
                JSONObject msg = new JSONObject();
                msg.put("role", m.isUser ? "user" : "assistant");
                msg.put("content", m.content);
                messages.put(msg);
            }
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);
        body.put("messages", messages);
        body.put("max_tokens", 1024);

        return postJson(urlStr, "Bearer " + apiKey.getApiKey(), body);
    }

    private String postJson(String urlStr, String authHeader, JSONObject body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authHeader != null && !authHeader.isEmpty()) conn.setRequestProperty("Authorization", authHeader);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new RuntimeException("HTTP " + code);
        String response = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        is.close();

        if (code != 200) {
            try {
                JSONObject err = new JSONObject(response);
                String msg = err.optString("error", response);
                if (msg.startsWith("{")) {
                    JSONObject errObj = err.optJSONObject("error");
                    if (errObj != null) msg = errObj.optString("message", msg);
                }
                throw new RuntimeException(msg);
            } catch (Exception e) {
                throw new RuntimeException("HTTP " + code + ": " + response);
            }
        }

        JSONObject json = new JSONObject(response);
        JSONArray choices = json.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            return choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim();
        }
        return "";
    }

    private String callAnthropic(String userMessage, List<ChatMessage> history) throws Exception {
        String base = "https://api.anthropic.com/v1/";
        String urlStr = base + "messages";

        JSONObject body = new JSONObject();
        body.put("model", apiKey.getModel() != null && !apiKey.getModel().isEmpty() ? apiKey.getModel() : "claude-3-haiku-20240307");
        body.put("max_tokens", 1024);

        JSONArray messages = new JSONArray();
        if (history != null) {
            for (ChatMessage m : history) {
                JSONObject msg = new JSONObject();
                msg.put("role", m.isUser ? "user" : "assistant");
                msg.put("content", m.content);
                messages.put(msg);
            }
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);
        body.put("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey.getApiKey());
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new RuntimeException("HTTP " + code);
        String response = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        is.close();

        if (code != 200) throw new RuntimeException("Anthropic: " + response);

        JSONObject json = new JSONObject(response);
        JSONArray content = json.optJSONArray("content");
        if (content != null && content.length() > 0) {
            return content.getJSONObject(0).optString("text", "").trim();
        }
        return "";
    }

    private String callGoogle(String userMessage, List<ChatMessage> history) throws Exception {
        String base = apiKey.getBaseUrl().trim();
        if (!base.endsWith("/")) base += "/";
        String model = apiKey.getModel() != null && !apiKey.getModel().isEmpty() ? apiKey.getModel() : "gemini-1.5-flash";
        String urlStr = base + "v1beta/models/" + model + ":generateContent?key=" + apiKey.getApiKey();

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();

        StringBuilder fullPrompt = new StringBuilder();
        if (history != null) {
            for (ChatMessage m : history) {
                fullPrompt.append(m.isUser ? "Kullanıcı: " : "Asistan: ").append(m.content).append("\n");
            }
        }
        fullPrompt.append("Kullanıcı: ").append(userMessage).append("\nAsistan: ");

        JSONObject part = new JSONObject();
        part.put("text", fullPrompt.toString());
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        body.put("contents", contents);
        body.put("generationConfig", new JSONObject()
                .put("maxOutputTokens", 1024)
                .put("temperature", 0.7f));

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new RuntimeException("HTTP " + code);
        String response = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        is.close();

        if (code != 200) throw new RuntimeException("Google: " + response);

        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates != null && candidates.length() > 0) {
            JSONArray parts = candidates.getJSONObject(0).optJSONObject("content").optJSONArray("parts");
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "").trim();
            }
        }
        return "";
    }

    /** Chat mesajı (geçmiş için). */
    public static class ChatMessage {
        public final boolean isUser;
        public final String content;

        public ChatMessage(boolean isUser, String content) {
            this.isUser = isUser;
            this.content = content;
        }
    }
}
