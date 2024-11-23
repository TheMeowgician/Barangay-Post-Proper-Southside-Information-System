package com.example.barangayinformationsystem;

public class DocumentRequestResponse {
    private boolean success;
    private String message;
    private int requestId;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getRequestId() { return requestId; }
}
