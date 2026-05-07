package com.example.quanlychitieu;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String id;
    private String title;
    private double amount;
    private String date;
    private String categoryId;
    private String type;
    private String note;
    private String relatedPerson;
    private boolean overLimit;
    private String overLimitType;
    public Transaction() {}

    public Transaction(String id, String title, double amount, String date, String categoryId, String type, String note) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.type = type;
        this.note = note;
        this.overLimit = false;
        this.overLimitType = "";
        this.relatedPerson = "";
    }
    public String getRelatedPerson() { return relatedPerson; }
    public void setRelatedPerson(String relatedPerson) { this.relatedPerson = relatedPerson; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getCategoryId() { return categoryId; }
    public String getType() { return type; }
    public String getNote() { return note; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setType(String type) { this.type = type; }
    public void setNote(String note) { this.note = note; }
    public boolean isOverLimit() {
        return overLimit;
    }

    public void setOverLimit(boolean overLimit) {
        this.overLimit = overLimit;
    }

    public String getOverLimitType() {
        return overLimitType;
    }

    public void setOverLimitType(String overLimitType) {
        this.overLimitType = overLimitType;
    }
}