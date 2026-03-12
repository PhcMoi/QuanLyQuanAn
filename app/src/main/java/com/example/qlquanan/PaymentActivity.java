package com.example.qlquanan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvTotalAmount;
    private Button btnConfirmPayment;
    private ImageButton btnBackPayment;
    private RadioGroup rgPaymentMethods;
    private ImageView imgVietQR;

    private String maHD;
    private String maBan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // --- 1. ÁNH XẠ GIAO DIỆN ---
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        btnBackPayment = findViewById(R.id.btnBackPayment);
        rgPaymentMethods = findViewById(R.id.rgPaymentMethods);
        imgVietQR = findViewById(R.id.imgVietQR);

        // --- 2. XỬ LÝ NÚT QUAY LẠI ---
        btnBackPayment.setOnClickListener(v -> finish());

        // --- 3. XỬ LÝ ẨN/HIỆN MÃ QR ---
        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbQR) {
                imgVietQR.setVisibility(View.VISIBLE); // Hiện mã QR khi chọn Chuyển khoản
            } else {
                imgVietQR.setVisibility(View.GONE); // Ẩn mã QR khi chọn Tiền mặt
            }
        });

        // --- 4. NHẬN DỮ LIỆU TỪ HÓA ĐƠN ---
        Intent intent = getIntent();
        maHD = intent.getStringExtra("MA_HD");
        maBan = intent.getStringExtra("MA_BAN");
        String total = intent.getStringExtra("TOTAL_AMOUNT");

        // Gắn chữ "Cần thanh toán: " vào số tiền
        if (total != null && !total.isEmpty()) {
            tvTotalAmount.setText("Cần thanh toán: " + total);
        }

        // --- 5. XÁC NHẬN THANH TOÁN ---
        btnConfirmPayment.setOnClickListener(v -> {
            if (maHD != null && maBan != null && !maHD.isEmpty() && !maBan.isEmpty()) {
                processPayment();
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy mã hóa đơn hoặc mã bàn!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processPayment() {
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("Đang xử lý hệ thống...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            String errorMessage = "Lỗi không xác định";
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    conn.setAutoCommit(false); // Bọc thép Transaction để chống lỗi đứt gãy

                    // --- SỬA LỖI ÉP KIỂU THỜI GIAN: CAST(GETDATE() AS TIME) ---
                    String updateBill = "UPDATE HoaDon SET ThoiGianDong = CAST(GETDATE() AS TIME) WHERE MaHD = ?";
                    PreparedStatement stmtBill = conn.prepareStatement(updateBill);
                    stmtBill.setString(1, maHD);
                    stmtBill.executeUpdate();
                    stmtBill.close();

                    // Giải phóng bàn
                    String updateTable = "UPDATE Ban SET TrangThai = 0 WHERE MaBan = ?";
                    PreparedStatement stmtTable = conn.prepareStatement(updateTable);
                    stmtTable.setString(1, maBan);
                    stmtTable.executeUpdate();
                    stmtTable.close();

                    conn.commit(); // Chốt cả 2 lệnh cùng lúc
                    conn.close();
                    isSuccess = true;
                } else {
                    errorMessage = "Không thể kết nối đến SQL Server";
                }
            } catch (Exception e) {
                errorMessage = e.getMessage(); // Bắt lỗi thật từ SQL Server
                Log.e("PAYMENT_ERROR", "Lỗi SQL: " + errorMessage);
            }

            final boolean finalSuccess = isSuccess;
            final String finalError = errorMessage;

            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(PaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

                    // Điều hướng đúng vai trò
                    SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
                    String role = pref.getString("USER_ROLE", "");

                    Intent nextIntent;
                    if (role.equals("admin") || role.equals("1")) {
                        nextIntent = new Intent(PaymentActivity.this, AdminActivity.class);
                    } else {
                        nextIntent = new Intent(PaymentActivity.this, TableActivity.class);
                    }

                    nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(nextIntent);
                    finish();
                } else {
                    btnConfirmPayment.setEnabled(true);
                    btnConfirmPayment.setText("XÁC NHẬN THANH TOÁN");

                    // HIỆN LỖI THẬT LÊN MÀN HÌNH ĐỂ DEBUG
                    new androidx.appcompat.app.AlertDialog.Builder(PaymentActivity.this)
                            .setTitle("🚨 Lỗi Hệ Thống")
                            .setMessage(finalError)
                            .setPositiveButton("Đóng", null)
                            .show();
                }
            });
        });
    }
}