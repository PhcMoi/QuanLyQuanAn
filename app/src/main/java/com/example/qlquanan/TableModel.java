package com.example.qlquanan;

public class TableModel {

    private String maBan;
    private String tenBan;
    private int trangThai;

    public TableModel(String maBan, String tenBan, int trangThai){
        this.maBan = maBan;
        this.tenBan = tenBan;
        this.trangThai = trangThai;
    }

    public String getMaBan() {
        return maBan;
    }

    public String getTenBan() {
        return tenBan;
    }

    public int getTrangThai() {
        return trangThai;
    }
}
