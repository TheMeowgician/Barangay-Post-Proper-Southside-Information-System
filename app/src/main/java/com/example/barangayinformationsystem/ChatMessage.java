package com.example.barangayinformationsystem;

public class ChatMessage {
    private int id;
    private int sender_id;
    private String message;
    private long timestamp;
    private boolean is_admin;

    public int getId() { return id; }
    public int getSenderId() { return sender_id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isAdmin() { return is_admin; }
}
