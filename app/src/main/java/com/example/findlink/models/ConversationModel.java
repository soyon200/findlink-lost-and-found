package com.example.findlink.models;

/**
 * ইনবক্সে দেখানো একটি কথোপকথন (chats/{chatId})।
 */
public class ConversationModel {

    private String chatId;
    private String itemId;
    private String itemTitle;
    private String itemImageUrl;
    private String postOwnerId;
    private String otherUserId;
    private String otherUserName;
    private String lastMessage;
    private String lastSenderId;
    private long lastMessageAt;
    private boolean unread;

    public ConversationModel() {
    }

    public String getChatId() { return chatId; }
    public String getItemId() { return itemId; }
    public String getItemTitle() { return itemTitle; }
    public String getItemImageUrl() { return itemImageUrl; }
    public String getPostOwnerId() { return postOwnerId; }
    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getLastMessage() { return lastMessage; }
    public String getLastSenderId() { return lastSenderId; }
    public long getLastMessageAt() { return lastMessageAt; }
    public boolean isUnread() { return unread; }

    public void setChatId(String chatId) { this.chatId = chatId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }
    public void setPostOwnerId(String postOwnerId) { this.postOwnerId = postOwnerId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public void setUnread(boolean unread) { this.unread = unread; }

    /**
     * অ্যাভাটারে দেখানোর জন্য নামের প্রথম দুটি অক্ষর।
     */
    public String getInitials() {
        if (otherUserName == null || otherUserName.trim().isEmpty()) {
            return "FL";
        }
        String[] parts = otherUserName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "FL" : initials.toString();
    }
}
