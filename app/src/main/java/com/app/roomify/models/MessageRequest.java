package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class MessageRequest {
    @SerializedName("message")
    private String message;

    @SerializedName("subject")
    private String subject;

    public MessageRequest(String message, String subject) {
        this.message = message;
        this.subject = subject;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}