package com.example.qlquanan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        boolean isLoggedIn = pref.getBoolean("IS_LOGGED_IN", false);
        String savedRole = pref.getString("USER_ROLE", "");

        // Chỉ tự động đăng nhập khi IS_LOGGED_IN là true
        if (isLoggedIn && !savedRole.isEmpty()) {
            if (savedRole.equals("admin") || savedRole.equals("1")) {
                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
            } else {
                startActivity(new Intent(LoginActivity.this, TableActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String user = edtUsername.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            checkLogin(user, pass);
        });
    }

    private void checkLogin(String username, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang kiểm tra...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            String userRole = "";
            String foundMaNV = "";
            String errorMsg = "";

            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    String query = "SELECT VaiTro, MaNV FROM TaiKhoan WHERE Username = ? AND Password = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, username);
                    stmt.setString(2, password);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        isSuccess = true;
                        userRole = rs.getString("VaiTro");
                        foundMaNV = rs.getString("MaNV");
                    } else {
                        errorMsg = "Sai tài khoản hoặc mật khẩu!";
                    }
                    conn.close();
                } else {
                    errorMsg = "Lỗi kết nối máy chủ!";
                }
            } catch (Exception e) {
                errorMsg = "Lỗi hệ thống: " + e.getMessage();
                Log.e("SQL_LOGIN", "Lỗi: " + errorMsg);
            }

            final boolean finalSuccess = isSuccess;
            final String finalRole = userRole;
            final String finalMaNV = foundMaNV;
            final String finalError = errorMsg;

            handler.post(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");

                if (finalSuccess) {
                    // --- BƯỚC 2: LƯU TRẠNG THÁI ĐĂNG NHẬP VÀO BỘ NHỚ ---
                    SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("USER_ID", finalMaNV);
                    editor.putString("USER_ROLE", finalRole != null ? finalRole.trim() : "");
                    editor.putBoolean("IS_LOGGED_IN", true); // Đánh dấu đã đăng nhập thành công
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    if (finalRole != null && (finalRole.trim().equals("1") || finalRole.trim().equalsIgnoreCase("admin"))) {
                        startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, TableActivity.class));
                    }
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, finalError, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}