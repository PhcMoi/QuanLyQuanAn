package com.example.qlquanan;

public class CategoryModel {
    private String maLoai;
    private String tenLoai;

    public CategoryModel(String maLoai, String tenLoai) {
        this.maLoai = maLoai;
        this.tenLoai = tenLoai;
    }

    public String getMaLoai() {
        return maLoai;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    // ĐÂY LÀ PHÉP THUẬT: Khi đưa vào Spinner, nó sẽ tự động gọi hàm này để lấy chữ hiển thị lên màn hình
    @Override
    public String toString() {
        return tenLoai;
    }
}