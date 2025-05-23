package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("type")
    private String type;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("related_id")
    private Integer relatedId;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Integer getRelatedId() { return relatedId; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRelatedId(Integer relatedId) { this.relatedId = relatedId; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}