package com.example.qlquanan;

public class BillModel {
    private String maHD;
    private String maBan;
    private String thoiGianDong;
    private double tongTien;

    public BillModel(String maHD, String maBan, String thoiGianDong, double tongTien) {
        this.maHD = maHD;
        this.maBan = maBan;
        this.thoiGianDong = thoiGianDong;
        this.tongTien = tongTien;
    }

    // Getters
    public String getMaHD() { return maHD; }
    public String getMaBan() { return maBan; }
    public String getThoiGianDong() { return thoiGianDong; }
    public double getTongTien() { return tongTien; }
}