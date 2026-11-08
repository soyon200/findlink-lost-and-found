package com.example.findlink.models;

public class InboxModel {
    private String type; // "MESSAGE" অথবা "CLAIM"
    private String content;
    private String status;

    public InboxModel() {}

    public InboxModel(String type, String content, String status) {
        this.type = type;
        this.content = content;
        this.status = status;
    }

    public String getType() { return type; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}