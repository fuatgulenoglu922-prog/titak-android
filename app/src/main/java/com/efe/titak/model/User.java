package com.efe.titak.model;

public class User {
    private String uid;
    private String googlePlayId;
    private String titakId; // Unique ID for friend search
    private String displayName;
    private String fcmToken;
    private boolean isOnline;

    public User() {
        // Required for Firebase
    }

    public User(String uid, String googlePlayId, String titakId, String displayName) {
        this.uid = uid;
        this.googlePlayId = googlePlayId;
        this.titakId = titakId;
        this.displayName = displayName;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getGooglePlayId() { return googlePlayId; }
    public void setGooglePlayId(String googlePlayId) { this.googlePlayId = googlePlayId; }

    public String getTitakId() { return titakId; }
    public void setTitakId(String titakId) { this.titakId = titakId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
