package com.example.findlink.models;

/**
 * একটি চ্যাট মেসেজ (chats/{chatId}/messages/{messageId})।
 */
public class ChatMessage {

    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private long createdAt;
    private String type; // CHAT অথবা CLAIM
    private boolean verified; // CLAIM হলে: সিকিউরিটি কোড দিয়ে মালিকানা প্রমাণ হয়েছে কি না

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderId, String senderName, String text,
                       long createdAt, String type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.createdAt = createdAt;
        this.type = type;
    }

    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getText() { return text; }
    public long getCreatedAt() { return createdAt; }
    public String getType() { return type; }
    public boolean isVerified() { return verified; }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setText(String text) { this.text = text; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setType(String type) { this.type = type; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isClaim() {
        return "CLAIM".equalsIgnoreCase(type);
    }
}
