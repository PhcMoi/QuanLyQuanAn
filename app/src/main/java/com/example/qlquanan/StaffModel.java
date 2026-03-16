package com.example.qlquanan;

public class StaffModel {
    private String maNV;
    private String tenNV;
    private String sdt;
    private String diaChi; // Đã vá lỗ hổng: Thêm biến chứa Địa chỉ
    private String username;
    private String vaiTro;
    private String password;

    // Cập nhật Constructor: Nhớ truyền đúng thứ tự từ hàm loadStaffData vào đây
    public StaffModel(String maNV, String tenNV, String sdt, String diaChi, String username, String vaiTro, String password) {
        this.maNV = maNV;
        this.tenNV = tenNV;
        this.sdt = sdt;
        this.diaChi = diaChi; // Gán dữ liệu
        this.username = username;
        this.vaiTro = vaiTro;
        this.password = password;
    }

    // Các hàm Get (Lấy dữ liệu ra)
    public String getMaNV() { return maNV; }
    public String getTenNV() { return tenNV; }
    public String getSdt() { return sdt; }
    public String getDiaChi() { return diaChi; } // Hàm lấy địa chỉ
    public String getUsername() { return username; }
    public String getVaiTro() { return vaiTro; }
    public String getPassword() { return password; }
}