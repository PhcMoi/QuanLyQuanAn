package com.example.qlquanan;

public class FoodModel {
    private String maMon;
    private String tenMon;
    private int gia; // Đóng vai trò là Giá đã giảm (GiaBan) để app tính tiền chính xác
    private String maLoai;
    private byte[] hinhAnh;
    private int trangThai;

    // THÊM 2 BIẾN MỚI CHO KHUYẾN MÃI
    private int giaGoc;
    private int phanTramGiam;

    // Cập nhật Constructor
    public FoodModel(String maMon, String tenMon, int gia, String maLoai, byte[] hinhAnh, int trangThai, int giaGoc, int phanTramGiam) {
        this.maMon = maMon;
        this.tenMon = tenMon;
        this.gia = gia;
        this.maLoai = maLoai;
        this.hinhAnh = hinhAnh;
        this.trangThai = trangThai;
        this.giaGoc = giaGoc;
        this.phanTramGiam = phanTramGiam;
    }

    public String getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public int getGia() { return gia; }
    public String getMaLoai() { return maLoai; }
    public byte[] getHinhAnh() { return hinhAnh; }
    public int getTrangThai() { return trangThai; }

    public int getGiaGoc() { return giaGoc; }
    public int getPhanTramGiam() { return phanTramGiam; }
}