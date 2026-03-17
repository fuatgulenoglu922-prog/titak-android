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

    public void setupLocalUser(String displayName, String titakId, String localUid) {
        String mockUid = localUid != null ? localUid : "LOCAL_" + Math.abs(displayName.hashCode());
        if (titakId == null) {
            titakId = generateUniqueTitakId(displayName);
        }
        currentUser = new User(mockUid, "LOCAL_PLAY", titakId, displayName);
        currentUser.setOnline(true);
        
        // Save to Firestore so other users can see them and they can send/receive requests
        db.collection("users").document(mockUid).set(currentUser);
    }

    private void updateFcmToken(String token) {
        if (currentUser == null || token == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        db.collection("users").document(currentUser.getUid()).update(data);
    }

    private String generateUniqueTitakId(String displayName) {
        // 4 haneli özel ID sistemi (1000-9999 arası) için daha geniş aralık
        int randomNum = new Random().nextInt(9000) + 1000;
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

    public void acceptFriendRequest(String requesterUid, String requesterName, SocialCallback callback) {
        if (currentUser == null) return;
        
        // Add to friends collection for current user
        Map<String, Object> friendData1 = new HashMap<>();
        friendData1.put("uid", requesterUid);
        friendData1.put("displayName", requesterName);
        friendData1.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(currentUser.getUid())
            .collection("friends").document(requesterUid).set(friendData1);
            
        // Add to friends collection for requester
        Map<String, Object> friendData2 = new HashMap<>();
        friendData2.put("uid", currentUser.getUid());
        friendData2.put("displayName", currentUser.getDisplayName());
        friendData2.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(requesterUid)
            .collection("friends").document(currentUser.getUid()).set(friendData2);
            
        // Remove request
        db.collection("users").document(currentUser.getUid())
            .collection("friendRequests").document(requesterUid).delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess("İstek kabul edildi"))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void rejectFriendRequest(String requesterUid, SocialCallback callback) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
            .collection("friendRequests").document(requesterUid).delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess("İstek reddedildi"))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface SocialCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
