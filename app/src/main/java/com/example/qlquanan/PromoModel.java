package com.example.qlquanan;

public class PromoModel {
    private String maKM, tenKM, ngayBatDau, ngayKetThuc;
    private int phanTramGiam, trangThai;

    public PromoModel(String maKM, String tenKM, int phanTramGiam, String ngayBatDau, String ngayKetThuc, int trangThai) {
        this.maKM = maKM;
        this.tenKM = tenKM;
        this.phanTramGiam = phanTramGiam;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.trangThai = trangThai;
    }

    public String getMaKM() { return maKM; }
    public String getTenKM() { return tenKM; }
    public int getPhanTramGiam() { return phanTramGiam; }
    public String getNgayBatDau() { return ngayBatDau; }
    public String getNgayKetThuc() { return ngayKetThuc; }
    public int getTrangThai() { return trangThai; }
}