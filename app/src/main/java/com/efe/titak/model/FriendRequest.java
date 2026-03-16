package com.efe.titak.model;

public class FriendRequest {
    private String id;
    private String fromUid;
    private String toUid;
    private String fromDisplayName;
    private String status; // pending, accepted, rejected
    private long timestamp;

    public FriendRequest() {
    }

    public FriendRequest(String id, String fromUid, String toUid, String fromDisplayName) {
        this.id = id;
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.fromDisplayName = fromDisplayName;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public String getFromDisplayName() { return fromDisplayName; }
    public void setFromDisplayName(String fromDisplayName) { this.fromDisplayName = fromDisplayName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
