package com.example.qlquanan;

public class StaffModel {
    private String maNV;
    private String tenNV;
    private String sdt;
    private String username;
    private String vaiTro;
    private String password;

    public StaffModel(String maNV, String tenNV, String sdt, String username, String vaiTro, String password) {
        this.maNV = maNV;
        this.tenNV = tenNV;
        this.sdt = sdt;
        this.username = username;
        this.vaiTro = vaiTro;
        this.password = password;
    }

    public String getMaNV() { return maNV; }
    public String getTenNV() { return tenNV; }
    public String getSdt() { return sdt; }
    public String getUsername() { return username; }
    public String getVaiTro() { return vaiTro; }
    public String getPassword() { return password; }
}