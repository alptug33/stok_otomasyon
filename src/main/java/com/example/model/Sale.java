package com.example.model;

import java.util.Date;

public class Sale {
    private int id;
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double totalAmount;
    private Date saleDate;
    private double profit;

    public Sale() {
        this.saleDate = new Date();
    }

    public Sale(int productId, String productName, int quantity, double unitPrice, double buyPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = quantity * unitPrice;
        this.profit = (unitPrice - buyPrice) * quantity;
        this.saleDate = new Date();
    }

    // Getter ve Setter metodları
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public Date getSaleDate() { return saleDate; }
    public void setSaleDate(Date saleDate) { this.saleDate = saleDate; }
    
    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
} 