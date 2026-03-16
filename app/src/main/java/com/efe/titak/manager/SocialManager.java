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
        String base = displayName.replaceAll("\\s+", "").toLowerCase();
        int randomNum = new Random().nextInt(9000) + 1000;
        return base + "#" + randomNum;
    }

    public User getCurrentUser() {
        return currentUser;
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
                            request.setLayout("fromUid", currentUser.getUid());
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
