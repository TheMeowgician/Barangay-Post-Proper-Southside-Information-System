package com.example.barangayinformationsystem;

import java.util.List;

public class MessageCheckResponse {
    private boolean hasNewMessages;
    private List<ChatMessage> newMessages;

    public boolean hasNewMessages() { return hasNewMessages; }
    public List<ChatMessage> getNewMessages() { return newMessages; }
}
