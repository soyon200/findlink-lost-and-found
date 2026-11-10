package com.example.findlink.models;

/**
 * একটি Lost & Found ডেস্ক — যেখানে পাওয়া জিনিস জমা রাখা যায়, অফিস/পুলিশ
 * মালিক খুঁজে বের করে।
 */
public class Office {

    private final String id;
    private final String name;
    private final String type;
    private final String address;
    private final String hours;

    public Office(String id, String name, String type, String address, String hours) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
        this.hours = hours;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getAddress() { return address; }
    public String getHours() { return hours; }
}
