package com.example.qlquanan;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private FoodModel foodItemActivity;
    private int quantity;

    // Sửa từ OrderItemActivity thành hàm khởi tạo OrderItem
    public OrderItem(FoodModel foodItemActivity, int quantity) {
        this.foodItemActivity = foodItemActivity;
        this.quantity = quantity;
    }

    public FoodModel getFoodItemActivity() { return foodItemActivity; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
