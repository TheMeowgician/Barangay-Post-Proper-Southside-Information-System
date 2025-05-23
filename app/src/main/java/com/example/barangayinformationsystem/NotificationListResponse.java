package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationListResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("notifications")
    private List<NotificationResponse> notifications;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public List<NotificationResponse> getNotifications() { return notifications; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setNotifications(List<NotificationResponse> notifications) { this.notifications = notifications; }
    public void setMessage(String message) { this.message = message; }
}