package com.example.qlquanan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvRole, tvGender, tvPhone, tvJoinDate, tvAddress;
    private Button btnLogout, btnChangePassword;
    private String currentMaNV;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. Ánh xạ View
        tvName = view.findViewById(R.id.tvProfileName);
        tvRole = view.findViewById(R.id.tvProfileRole);
        tvGender = view.findViewById(R.id.tvProfileGender);
        tvPhone = view.findViewById(R.id.tvProfilePhone);
        tvJoinDate = view.findViewById(R.id.tvProfileJoinDate);
        tvAddress = view.findViewById(R.id.tvProfileAddress);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        // 2. Lấy MaNV từ bộ nhớ máy
        SharedPreferences pref = getActivity().getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        currentMaNV = pref.getString("USER_ID", "");

        if (!currentMaNV.isEmpty()) {
            loadEmployeeProfile();
        } else {
            Toast.makeText(getContext(), "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
        }

        // 3. Xử lý Đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // 4. Xử lý Đăng xuất
        btnLogout.setOnClickListener(v -> {
            pref.edit().clear().apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
            Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadEmployeeProfile() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    String sql = "SELECT TenNV, Phai, SDT, NgayVaoLam, DiaChi FROM NhanVien WHERE MaNV = ?";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setString(1, currentMaNV);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        String name = rs.getString("TenNV");
                        boolean gender = rs.getBoolean("Phai");
                        String phone = rs.getString("SDT");
                        String joinDate = rs.getString("NgayVaoLam");
                        String address = rs.getString("DiaChi");

                        handler.post(() -> {
                            tvName.setText(name != null ? name : "Chưa cập nhật tên");
                            tvRole.setText("Mã nhân viên: " + currentMaNV);
                            tvGender.setText("Giới tính: " + (gender ? "Nam" : "Nữ"));
                            tvPhone.setText("SĐT: " + (phone != null && !phone.isEmpty() ? phone : "Chưa cập nhật"));
                            tvAddress.setText("Địa chỉ: " + (address != null && !address.isEmpty() ? address : "Chưa cập nhật"));

                            if (joinDate != null && joinDate.length() >= 10) {
                                tvJoinDate.setText("Ngày vào làm: " + joinDate.substring(0, 10));
                            } else {
                                tvJoinDate.setText("Ngày vào làm: --/--/----");
                            }
                        });
                    }
                    rs.close(); pst.close(); conn.close();
                }
            } catch (Exception e) {
                Log.e("SQL_PROFILE", "Lỗi: " + e.getMessage());
            }
        });
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        // Chống chạm nhầm ra ngoài để tắt Popup
        dialog.setCanceledOnTouchOutside(false);

        EditText edtOldPass = dialogView.findViewById(R.id.edtOldPassword);
        EditText edtNewPass = dialogView.findViewById(R.id.edtNewPassword);
        EditText edtConfirmPass = dialogView.findViewById(R.id.edtConfirmPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        Button btnSave = dialogView.findViewById(R.id.btnSavePassword);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String oldPass = edtOldPass.getText().toString().trim();
            String newPass = edtNewPass.getText().toString().trim();
            String confirmPass = edtConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(getContext(), "Mật khẩu mới không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chống Spam Click
            btnSave.setEnabled(false);
            btnSave.setText("Đang xử lý...");

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean isSuccess = false;
                String errorMsg = "";

                try {
                    Connection conn = new SQLConnection().connection();
                    if (conn != null) {
                        String updateSql = "UPDATE TaiKhoan SET Password = ? WHERE MaNV = ? AND Password = ?";
                        PreparedStatement pst = conn.prepareStatement(updateSql);
                        pst.setString(1, newPass);
                        pst.setString(2, currentMaNV);
                        pst.setString(3, oldPass);

                        int rowsAffected = pst.executeUpdate();
                        if (rowsAffected > 0) {
                            isSuccess = true;
                        } else {
                            errorMsg = "Mật khẩu hiện tại không đúng!";
                        }
                        pst.close(); conn.close();
                    } else {
                        errorMsg = "Lỗi kết nối máy chủ!";
                    }
                } catch (Exception e) {
                    errorMsg = "Lỗi SQL: " + e.getMessage();
                }

                final boolean finalSuccess = isSuccess;
                final String finalError = errorMsg;

                handler.post(() -> {
                    if (finalSuccess) {
                        dialog.dismiss();

                        // Hiện bảng thông báo ép đăng xuất
                        new android.app.AlertDialog.Builder(getContext())
                                .setTitle("Thành công")
                                .setMessage("Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới để tiếp tục.")
                                .setCancelable(false)
                                .setPositiveButton("Đồng ý", (d, w) -> {
                                    SharedPreferences pref = getActivity().getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
                                    pref.edit().clear().apply();

                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                })
                                .show();
                    } else {
                        // Mở khóa nút nếu thất bại
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu thay đổi");
                        Toast.makeText(getContext(), finalError, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }
}