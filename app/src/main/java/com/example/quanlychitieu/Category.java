package com.example.quanlychitieu;

public class Category {
    private String id;
    private String name;
    private String iconName;
    private String amount;
    private boolean deleted;

    public Category() {}

    public Category(String id, String name, String iconName, String amount) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.amount = amount;
        this.deleted = false;
    }

    public Category(String id, String name, String iconName, String amount, boolean deleted) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.amount = amount;
        this.deleted = deleted;
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
}