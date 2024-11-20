package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

public class RegistrationResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}