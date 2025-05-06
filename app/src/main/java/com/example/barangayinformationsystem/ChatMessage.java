package com.example.barangayinformationsystem;

public class ChatMessage {
    private int id;
    private int sender_id;
    private Integer admin_id;
    private String message;
    private long timestamp;
    private boolean is_admin;
    private String sender_name;
    private boolean is_auto_response; // New field for auto-responses

    public ChatMessage() {
        // Default constructor for retrofit
    }

    // Constructor for creating auto-response messages locally
    public ChatMessage(String message, long timestamp, boolean isAutoResponse) {
        this.message = message;
        this.timestamp = timestamp;
        this.is_admin = true; // Auto-responses appear as admin messages
        this.sender_name = "Barangay Auto-Response";
        this.is_auto_response = isAutoResponse;
        this.id = -1; // Use negative ID to avoid conflicts with server IDs
    }

    public int getId() { return id; }
    public int getSenderId() { return sender_id; }
    public Integer getAdminId() { return admin_id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isAdmin() { return is_admin; }
    public String getSenderName() { return sender_name; }
    public boolean isAutoResponse() { return is_auto_response; }

    public void setId(int id) { this.id = id; }
}