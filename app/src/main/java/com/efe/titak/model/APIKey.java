package com.efe.titak.model;

import java.io.Serializable;

public class APIKey implements Serializable {
    private int id;
    private String name;
    private String apiKey;
    private String baseUrl;
    private String model;
    private long createdAt;
    private boolean isActive;

    public APIKey() {
    }

    public APIKey(int id, String name, String apiKey, String baseUrl, String model, long createdAt, boolean isActive) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
