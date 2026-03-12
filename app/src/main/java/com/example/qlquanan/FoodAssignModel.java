package com.example.qlquanan;

public class FoodAssignModel {
    private String maMon;
    private String tenMon;
    private double giaGoc;
    private String maKMDangCo;
    private boolean isSelected; // Biến "sinh tử" để chống lỗi tái chế Checkbox

    public FoodAssignModel(String maMon, String tenMon, double giaGoc, String maKMDangCo, boolean isSelected) {
        this.maMon = maMon;
        this.tenMon = tenMon;
        this.giaGoc = giaGoc;
        this.maKMDangCo = maKMDangCo;
        this.isSelected = isSelected;
    }

    public String getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public double getGiaGoc() { return giaGoc; }
    public String getMaKMDangCo() { return maKMDangCo; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}