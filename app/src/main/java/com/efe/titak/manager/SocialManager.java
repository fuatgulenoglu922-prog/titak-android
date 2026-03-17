package com.efe.titak.manager;

import com.efe.titak.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SocialManager {
    private static SocialManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private User currentUser;

    private SocialManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized SocialManager getInstance() {
        if (instance == null) {
            instance = new SocialManager();
        }
        return instance;
    }

    public void syncUserProfile(String googlePlayId, String displayName, String fcmToken) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentUser = documentSnapshot.toObject(User.class);
                updateFcmToken(fcmToken);
            } else {
                // Create new user with unique Titak ID
                String titakId = generateUniqueTitakId(displayName);
                currentUser = new User(uid, googlePlayId, titakId, displayName);
                currentUser.setFcmToken(fcmToken);
                currentUser.setOnline(true);
                
                db.collection("users").document(uid).set(currentUser);
            }
        });
    }

    private void updateFcmToken(String token) {
        if (currentUser == null || token == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        db.collection("users").document(currentUser.getUid()).update(data);
    }

    private String generateUniqueTitakId(String displayName) {
        // 3 haneli özel ID sistemi (100-999 arası)
        int randomNum = new Random().nextInt(900) + 100;
        return String.valueOf(randomNum);
    }

    public User getCurrentUser() {
        if (currentUser == null && auth.getCurrentUser() != null) {
            // Try to recover from recent auth but missing sync (edge case)
            syncUserProfile("GP_RECOVERED", auth.getCurrentUser().getDisplayName(), null);
        }
        return currentUser;
    }

    public void sendMessage(String targetUid, String text, SocialCallback callback) {
        if (currentUser == null) {
            callback.onError("Giriş yapılmamış");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("fromUid", currentUser.getUid());
        message.put("text", text);
        message.put("timestamp", System.currentTimeMillis());

        // Save to a shared "chats" collection or subcollection
        String chatId = getChatId(currentUser.getUid(), targetUid);
        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> callback.onSuccess("Mesaj gönderildi"))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public void sendFriendRequest(String targetTitakId, SocialCallback callback) {
        db.collection("users")
                .whereEqualTo("titakId", targetTitakId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User targetUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (targetUser != null) {
                            Map<String, Object> request = new HashMap<>();
                            request.put("fromUid", currentUser.getUid());
                            request.put("fromDisplayName", currentUser.getDisplayName());
                            request.put("status", "pending");
                            request.put("timestamp", System.currentTimeMillis());

                            db.collection("users")
                                    .document(targetUser.getUid())
                                    .collection("friendRequests")
                                    .document(currentUser.getUid())
                                    .set(request)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess("İstek gönderildi"))
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        }
                    } else {
                        callback.onError("Kullanıcı bulunamadı");
                    }
                });
    }

    public interface SocialCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
