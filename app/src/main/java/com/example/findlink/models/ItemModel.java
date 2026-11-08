package com.example.findlink.models;

public class ItemModel {
    private String itemId;
    private String userId;
    private String title;
    private String category;
    private String location;
    private String description;
    private String reportType;
    private String status;
    private String phone;
    private long timestamp; // পোস্ট করার সময় ট্র্যাক করার জন্য নতুন ফিল্ড
    private String imageUrl; // imgbb-তে আপলোড করা ছবির লিংক (না থাকলে null)
    private String securityHash; // LOST পোস্টে দেওয়া সিকিউরিটি কোডের salted SHA-256 হ্যাশ
    private String claimCode; // FOUND পোস্টে অ্যাপ নিজেই বানানো ৬ ডিজিটের হ্যান্ডওভার কোড
    private String securityCode; // LOST পোস্টে অ্যাপ-বানানো কোড (শুধু মালিক নিজের স্ক্রিনে দেখেন)

    public ItemModel() {}

    public ItemModel(String itemId, String userId, String title, String category, String location, String description, String reportType, String status, String phone, long timestamp) {
        this.itemId = itemId;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.location = location;
        this.description = description;
        this.reportType = reportType;
        this.status = status;
        this.phone = phone;
        this.timestamp = timestamp;
    }

    public ItemModel(String itemId, String userId, String title, String category, String location, String description,
                     String reportType, String status, String phone, long timestamp, String imageUrl) {
        this(itemId, userId, title, category, location, description, reportType, status, phone, timestamp);
        this.imageUrl = imageUrl;
    }

    public String getItemId() { return itemId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getReportType() { return reportType; }
    public String getStatus() { return status; }
    public String getPhone() { return phone; }
    public long getTimestamp() { return timestamp; }
    public String getImageUrl() { return imageUrl; }
    public String getSecurityHash() { return securityHash; }
    public String getClaimCode() { return claimCode; }
    public String getSecurityCode() { return securityCode; }

    public void setPhone(String phone) { this.phone = phone; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSecurityHash(String securityHash) { this.securityHash = securityHash; }
    public void setClaimCode(String claimCode) { this.claimCode = claimCode; }
    public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }
}