package com.example.quanlychitieu;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private double totalBalance;

    public User() {}

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.totalBalance = 0.0;
        this.phone = "";
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getTotalBalance() { return totalBalance; }
    public void setTotalBalance(double totalBalance) { this.totalBalance = totalBalance; }
}
