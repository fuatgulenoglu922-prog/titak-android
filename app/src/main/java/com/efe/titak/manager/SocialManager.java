package com.efe.titak.manager;

import android.util.Log;
import com.efe.titak.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SocialManager {
    private static final String TAG = "SocialManager";
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

    public void setupLocalUser(String displayName, String titakId, String localUid) {
        String uid = localUid != null ? localUid : "USER_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        
        String finalTitakId = titakId;
        if (finalTitakId == null || finalTitakId.isEmpty()) {
            finalTitakId = String.valueOf(new Random().nextInt(9000) + 1000);
        }

        currentUser = new User(uid, "LOCAL", finalTitakId, displayName);
        currentUser.setOnline(true);
        
        db.collection("users").document(uid).set(currentUser, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Kullanıcı Firestore'a kaydedildi: " + displayName))
            .addOnFailureListener(e -> Log.e(TAG, "Firestore kayıt hatası", e));
            
        Map<String, Object> index = new HashMap<>();
        index.put("uid", uid);
        db.collection("id_index").document(finalTitakId).set(index);
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

        String chatId = getChatId(currentUser.getUid(), targetUid);
        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> callback.onSuccess("Mesaj gönderildi"))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendFriendRequest(String targetTitakId, SocialCallback callback) {
        if (currentUser == null) {
            callback.onError("Önce giriş yapmalısınız");
            return;
        }

        if (targetTitakId.equals(currentUser.getTitakId())) {
            callback.onError("Kendinizi ekleyemezsiniz");
            return;
        }

        db.collection("id_index").document(targetTitakId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String targetUid = doc.getString("uid");
                if (targetUid != null) {
                    executeFriendRequest(targetUid, callback);
                }
            } else {
                db.collection("users")
                    .whereEqualTo("titakId", targetTitakId)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            String targetUid = query.getDocuments().get(0).getId();
                            executeFriendRequest(targetUid, callback);
                        } else {
                            callback.onError("Kullanıcı bulunamadı (" + targetTitakId + ")");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Arama hatası: " + e.getMessage()));
            }
        });
    }

    private void executeFriendRequest(String targetUid, SocialCallback callback) {
        Map<String, Object> request = new HashMap<>();
        request.put("fromUid", currentUser.getUid());
        request.put("fromDisplayName", currentUser.getDisplayName());
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(targetUid)
                .collection("friendRequests").document(currentUser.getUid())
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Arkadaşlık isteği gönderildi!"))
                .addOnFailureListener(e -> callback.onError("İstek gönderilemedi"));
    }

    public void acceptFriendRequest(String requesterUid, String requesterName, SocialCallback callback) {
        if (currentUser == null) return;
        
        long now = System.currentTimeMillis();
        
        Map<String, Object> friendData = new HashMap<>();
        friendData.put("uid", requesterUid);
        friendData.put("displayName", requesterName);
        friendData.put("since", now);
        
        db.collection("users").document(currentUser.getUid()).collection("friends").document(requesterUid).set(friendData);
        
        Map<String, Object> myData = new HashMap<>();
        myData.put("uid", currentUser.getUid());
        myData.put("displayName", currentUser.getDisplayName());
        myData.put("since", now);
        
        db.collection("users").document(requesterUid).collection("friends").document(currentUser.getUid()).set(myData);
            
        db.collection("users").document(currentUser.getUid()).collection("friendRequests").document(requesterUid).delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess("Arkadaş eklendi!"))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public interface SocialCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
