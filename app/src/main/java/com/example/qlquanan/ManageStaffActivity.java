package com.example.qlquanan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageStaffActivity extends AppCompatActivity {

    private RecyclerView rvStaffList;
    private StaffAdapter staffAdapter;
    private List<StaffModel> staffList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_staff);

        findViewById(R.id.btnBackAdmin).setOnClickListener(v -> finish());

        ImageButton btnAddStaffTop = findViewById(R.id.btnAddStaffTop);
        btnAddStaffTop.setOnClickListener(v -> showAddStaffDialog());

        rvStaffList = findViewById(R.id.rvStaffList);
        rvStaffList.setLayoutManager(new LinearLayoutManager(this));
        staffList = new ArrayList<>();

        staffAdapter = new StaffAdapter(this, staffList, () -> loadStaffData(), staff -> showEditStaffDialog(staff));
        rvStaffList.setAdapter(staffAdapter);

        loadStaffData();
    }

    private void loadStaffData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<StaffModel> tempList = new ArrayList<>();
            boolean isSuccess = false;

            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // Lấy những nhân viên đang có TrangThai = 1 (Đang làm việc)
                    String sql = "SELECT nv.MaNV, nv.TenNV, nv.SDT, nv.DiaChi, tk.Username, tk.VaiTro, tk.Password " +
                            "FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaNV = tk.MaNV WHERE nv.TrangThai = 1";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    while (rs.next()) {
                        tempList.add(new StaffModel(
                                rs.getString("MaNV"), rs.getString("TenNV"), rs.getString("SDT"),
                                rs.getString("DiaChi"), // Thêm dòng này để hứng dữ liệu
                                rs.getString("Username"), rs.getString("VaiTro"), rs.getString("Password")
                        ));
                    }
                    rs.close(); stmt.close(); conn.close();
                    isSuccess = true;
                }
            } catch (Exception e) {
                Log.e("SQL_LOAD", "Lỗi: " + e.getMessage());
            }

            // ĐÓNG BĂNG BIẾN ĐỂ TRÁNH LỖI LAMBDA
            final boolean finalSuccess = isSuccess;

            handler.post(() -> {
                if (finalSuccess) {
                    staffList.clear();
                    staffList.addAll(tempList);
                    staffAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "Lỗi tải danh sách!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ==========================================
    // KHU VỰC 1: THÊM NHÂN VIÊN MỚI
    // ==========================================
    private void showAddStaffDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_staff, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText edtFullName = dialogView.findViewById(R.id.edtFullName);
        EditText edtPhone = dialogView.findViewById(R.id.edtPhone);
        EditText edtAddress = dialogView.findViewById(R.id.edtAddress);
        TextView tvJoinDate = dialogView.findViewById(R.id.tvJoinDate);
        android.widget.RadioButton rbMale = dialogView.findViewById(R.id.rbMale);

        EditText edtUsername = dialogView.findViewById(R.id.edtUsername);
        EditText edtPassword = dialogView.findViewById(R.id.edtPassword);
        android.widget.RadioButton rbAdmin = dialogView.findViewById(R.id.rbAdmin);
        Button btnSaveEmployee = dialogView.findViewById(R.id.btnSaveEmployee);

        tvJoinDate.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                tvJoinDate.setText(year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth));
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        btnSaveEmployee.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String joinDate = tvJoinDate.getText().toString().trim();
            boolean isMale = rbMale.isChecked();
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String role = rbAdmin.isChecked() ? "1" : "0";

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || joinDate.contains("Bấm để chọn")) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveEmployee.setEnabled(false);
            btnSaveEmployee.setText("Đang lưu...");

            saveStaffToDatabase(fullName, phone, address, joinDate, isMale, username, password, role, dialog, btnSaveEmployee);
        });
        dialog.show();
    }

    private void saveStaffToDatabase(String tenNV, String sdt, String diaChi, String ngayVaoLam, boolean isMale, String user, String pass, String role, AlertDialog dialog, Button btnSave) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean isSuccess = false; String error = ""; Connection conn = null;
            try {
                conn = new SQLConnection().connection();
                if (conn != null) {
                    conn.setAutoCommit(false); // Bật khiên bảo vệ Transaction

                    // 1. Check User trùng lặp
                    PreparedStatement stmtCheck = conn.prepareStatement("SELECT Username FROM TaiKhoan WHERE Username = ?");
                    stmtCheck.setString(1, user);
                    if (stmtCheck.executeQuery().next()) throw new Exception("Tên đăng nhập đã tồn tại!");
                    stmtCheck.close();

                    // 2. INSERT Nhân Viên & Bắt lấy mã SQL vừa tự sinh ra
                    // TUYỆT ĐỐI KHÔNG INSERT MaNV, nhưng dùng OUTPUT INSERTED.MaNV để hứng mã về
                    String sqlInsertNV = "INSERT INTO NhanVien (TenNV, Phai, SDT, NgayVaoLam, DiaChi, TrangThai) " +
                            "OUTPUT INSERTED.MaNV " +
                            "VALUES (?, ?, ?, ?, ?, 1)";

                    PreparedStatement stmtNV = conn.prepareStatement(sqlInsertNV);
                    stmtNV.setString(1, tenNV);
                    stmtNV.setBoolean(2, isMale);
                    stmtNV.setString(3, sdt.isEmpty() ? null : sdt);
                    stmtNV.setString(4, ngayVaoLam);
                    stmtNV.setString(5, diaChi.isEmpty() ? null : diaChi);

                    // Vì có mệnh đề OUTPUT trả về data, ta phải dùng executeQuery thay vì executeUpdate
                    ResultSet rsNV = stmtNV.executeQuery();
                    String newMaNV = "";
                    if (rsNV.next()) {
                        newMaNV = rsNV.getString("MaNV"); // Chộp ngay cái mã NV... vừa ra lò!
                    }
                    rsNV.close();
                    stmtNV.close();

                    if (newMaNV.isEmpty()) throw new Exception("Hệ thống SQL không thể tự sinh mã nhân viên!");

                    // 3. INSERT Tài Khoản bằng chính cái mã vừa chộp được
                    PreparedStatement stmtTK = conn.prepareStatement("INSERT INTO TaiKhoan (Username, Password, VaiTro, MaNV) VALUES (?, ?, ?, ?)");
                    stmtTK.setString(1, user);
                    stmtTK.setString(2, pass);
                    stmtTK.setString(3, role); // role lúc này đã là "1" hoặc "0" nhờ bản fix trước
                    stmtTK.setString(4, newMaNV);
                    stmtTK.executeUpdate();
                    stmtTK.close();

                    // Chốt giao dịch thành công
                    conn.commit();
                    isSuccess = true;
                }
            } catch (Exception e) {
                error = e.getMessage();
                try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            } finally {
                try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception ex) {}
            }

            final boolean fSuccess = isSuccess; final String fError = error;
            handler.post(() -> {
                if (fSuccess) { Toast.makeText(this, "Thêm nhân viên thành công!", Toast.LENGTH_SHORT).show(); dialog.dismiss(); loadStaffData(); }
                else {
                    Toast.makeText(this, "Lỗi: " + fError, Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true); btnSave.setText("LƯU DỮ LIỆU");
                }
            });
        });
    }

    // ==========================================
    // KHU VỰC 2: SỬA NHÂN VIÊN
    // ==========================================
    private void showEditStaffDialog(StaffModel staff) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_staff, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText edtFullName = dialogView.findViewById(R.id.edtFullName);
        EditText edtPhone = dialogView.findViewById(R.id.edtPhone);
        EditText edtAddress = dialogView.findViewById(R.id.edtAddress);
        TextView tvJoinDate = dialogView.findViewById(R.id.tvJoinDate);

        EditText edtUsername = dialogView.findViewById(R.id.edtUsername);
        EditText edtPassword = dialogView.findViewById(R.id.edtPassword);
        android.widget.RadioButton rbAdmin = dialogView.findViewById(R.id.rbAdmin);
        android.widget.RadioButton rbStaff = dialogView.findViewById(R.id.rbStaff);
        Button btnSaveEmployee = dialogView.findViewById(R.id.btnSaveEmployee);

        if (tvTitle != null) tvTitle.setText("Sửa Thông Tin: " + staff.getTenNV());
        edtFullName.setText(staff.getTenNV());
        edtPhone.setText(staff.getSdt());
        edtFullName.setText(staff.getTenNV());
        edtPhone.setText(staff.getSdt());
        tvJoinDate.setText("Bấm để đổi ngày làm (Tùy chọn)");

        edtUsername.setText(staff.getUsername());
        edtUsername.setEnabled(false);
        edtUsername.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));

        if (staff.getDiaChi() != null) {
            edtAddress.setText(staff.getDiaChi());
        }

        if (staff.getVaiTro() != null && staff.getVaiTro().equals("1")) {
            rbAdmin.setChecked(true);
        } else {
            rbStaff.setChecked(true);
        }

        btnSaveEmployee.setText("CẬP NHẬT DỮ LIỆU");

        tvJoinDate.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                tvJoinDate.setText(year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth));
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        btnSaveEmployee.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim(); // Bổ sung lấy Địa chỉ
            String pass = edtPassword.getText().toString().trim();

            // ĐÃ FIX: Chuyển thành số 1 và 0 để SQL Server nuốt trôi
            String role = rbAdmin.isChecked() ? "1" : "0";

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Tên nhân viên không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveEmployee.setEnabled(false);
            btnSaveEmployee.setText("Đang cập nhật...");

            // Truyền thêm address vào hàm
            updateStaffInDatabase(staff.getMaNV(), fullName, phone, address, pass, role, dialog, btnSaveEmployee);
        });
        dialog.show();
    }

    // Đã thêm String address vào tham số
    private void updateStaffInDatabase(String maNV, String tenNV, String sdt, String address, String pass, String role, AlertDialog dialog, Button btnSave) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean isSuccess = false; String error = ""; Connection conn = null;
            try {
                conn = new SQLConnection().connection();
                if (conn != null) {
                    conn.setAutoCommit(false);

                    // 1. Update bảng NhanVien (Bổ sung thêm cột DiaChi)
                    PreparedStatement stmtNV = conn.prepareStatement("UPDATE NhanVien SET TenNV = ?, SDT = ?, DiaChi = ? WHERE MaNV = ?");
                    stmtNV.setString(1, tenNV);
                    stmtNV.setString(2, sdt);
                    stmtNV.setString(3, address); // Lưu địa chỉ mới
                    stmtNV.setString(4, maNV);
                    stmtNV.executeUpdate(); stmtNV.close();

                    // 2. Update bảng TaiKhoan (role bây giờ là "0" hoặc "1")
                    String sqlTK = "UPDATE TaiKhoan SET VaiTro = ? " + (pass.isEmpty() ? "" : ", Password = ? ") + "WHERE MaNV = ?";
                    PreparedStatement stmtTK = conn.prepareStatement(sqlTK);
                    stmtTK.setString(1, role);
                    if (pass.isEmpty()) {
                        stmtTK.setString(2, maNV);
                    } else {
                        stmtTK.setString(2, pass);
                        stmtTK.setString(3, maNV);
                    }
                    stmtTK.executeUpdate(); stmtTK.close();

                    conn.commit(); isSuccess = true;
                }
            } catch (Exception e) {
                error = e.getMessage();
                try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            } finally {
                try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception ex) {}
            }

            final boolean fSuccess = isSuccess; final String fError = error;
            handler.post(() -> {
                if (fSuccess) { Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show(); dialog.dismiss(); loadStaffData(); }
                else {
                    Toast.makeText(this, "Lỗi: " + fError, Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true); btnSave.setText("CẬP NHẬT DỮ LIỆU");
                }
            });
        });
    }
}