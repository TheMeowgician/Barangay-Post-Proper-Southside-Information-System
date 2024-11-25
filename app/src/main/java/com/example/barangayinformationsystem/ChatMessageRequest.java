package com.example.barangayinformationsystem;

public class ChatMessageRequest {
    private int sender_id;
    private String message;

    public ChatMessageRequest(int sender_id, String message) {
        this.sender_id = sender_id;
        this.message = message;
    }
}