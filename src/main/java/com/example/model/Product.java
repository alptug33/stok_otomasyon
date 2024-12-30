package com.example.model;

public class Product {
    private int id;
    private String name;
    private double buyPrice;
    private double sellPrice;
    private int quantity;
    private int criticalLevel;
    private String barcode;
    private String supplier;

    public Product() {}

    public Product(String name, double buyPrice, double sellPrice, int quantity) {
        this.name = name;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.criticalLevel = 10; // Varsayılan kritik seviye
    }

    // Getter ve Setter metodları
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    
    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public int getCriticalLevel() { return criticalLevel; }
    public void setCriticalLevel(int criticalLevel) { this.criticalLevel = criticalLevel; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    public double getTotalValue() {
        return buyPrice * quantity;
    }
    
    public boolean isLowStock() {
        return quantity <= criticalLevel;
    }
} 