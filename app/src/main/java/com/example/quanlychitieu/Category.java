package com.example.quanlychitieu;

public class Category {
    private String id;
    private String name;
    private String iconName;
    private String amount;
    private boolean deleted;
    private String type; // "thu" hoặc "chi"

    public Category() {}

    public Category(String id, String name, String iconName, String amount) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.amount = amount;
        this.deleted = false;
        this.type = "chi";
    }

    public Category(String id, String name, String iconName, String amount, boolean deleted) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.amount = amount;
        this.deleted = deleted;
        this.type = "chi";
    }

    public Category(String id, String name, String iconName, String amount, boolean deleted, String type) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.amount = amount;
        this.deleted = deleted;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}