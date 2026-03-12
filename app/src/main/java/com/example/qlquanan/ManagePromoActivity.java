package com.example.qlquanan;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagePromoActivity extends AppCompatActivity {

    private RecyclerView rvPromoList;
    private PromoAdapter promoAdapter;
    private List<PromoModel> promoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_promo);

        findViewById(R.id.btnBackAdmin).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddPromoTop).setOnClickListener(v -> showAddPromoDialog());

        rvPromoList = findViewById(R.id.rvPromoList);
        rvPromoList.setLayoutManager(new LinearLayoutManager(this));

        promoList = new ArrayList<>();
        promoAdapter = new PromoAdapter(this, promoList);
        rvPromoList.setAdapter(promoAdapter);

        loadPromoData();
    }

    private void loadPromoData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            List<PromoModel> tempList = new ArrayList<>();

            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // Lấy tất cả mã khuyến mãi đang hiển thị (sắp xếp cái mới nhất lên đầu)
                    String sql = "SELECT * FROM KhuyenMai ORDER BY NgayKetThuc DESC";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    while (rs.next()) {
                        tempList.add(new PromoModel(
                                rs.getString("MaKM"), rs.getString("TenKM"),
                                rs.getInt("PhanTramGiam"), rs.getString("NgayBatDau"),
                                rs.getString("NgayKetThuc"), rs.getInt("TrangThai")
                        ));
                    }
                    rs.close(); stmt.close(); conn.close();
                    isSuccess = true;
                }
            } catch (Exception e) {
                Log.e("SQL_PROMO", "Lỗi tải danh sách: " + e.getMessage());
            }

            final boolean finalSuccess = isSuccess;
            handler.post(() -> {
                if (finalSuccess) {
                    promoList.clear();
                    promoList.addAll(tempList);
                    promoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "Không thể tải danh sách khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showAddPromoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_promo, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        EditText edtPromoCode = dialogView.findViewById(R.id.edtPromoCode);
        EditText edtPromoName = dialogView.findViewById(R.id.edtPromoName);
        EditText edtPromoPercent = dialogView.findViewById(R.id.edtPromoPercent);
        TextView tvPromoStartDate = dialogView.findViewById(R.id.tvPromoStartDate);
        TextView tvPromoEndDate = dialogView.findViewById(R.id.tvPromoEndDate);
        Button btnSavePromo = dialogView.findViewById(R.id.btnSavePromo);

        View.OnClickListener datePickerListener = v -> {
            TextView targetTextView = (TextView) v;
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String selectedDate = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth);
                targetTextView.setText(selectedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        };

        tvPromoStartDate.setOnClickListener(datePickerListener);
        tvPromoEndDate.setOnClickListener(datePickerListener);

        btnSavePromo.setOnClickListener(v -> {
            String maKM = edtPromoCode.getText().toString().trim().toUpperCase();
            String tenKM = edtPromoName.getText().toString().trim();
            String percentStr = edtPromoPercent.getText().toString().trim();
            String startDate = tvPromoStartDate.getText().toString().trim();
            String endDate = tvPromoEndDate.getText().toString().trim();

            if (maKM.isEmpty() || tenKM.isEmpty() || percentStr.isEmpty() || startDate.contains("Ngày") || endDate.contains("Ngày")) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            int percent = Integer.parseInt(percentStr);
            if (percent <= 0 || percent > 100) {
                Toast.makeText(this, "Phần trăm giảm phải từ 1 đến 100!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDate.compareTo(endDate) > 0) {
                Toast.makeText(this, "Ngày kết thúc không được nhỏ hơn ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSavePromo.setEnabled(false);
            btnSavePromo.setText("Đang lưu...");

            savePromoToDatabase(maKM, tenKM, percent, startDate, endDate, dialog, btnSavePromo);
        });

        dialog.show();
    }

    private void savePromoToDatabase(String maKM, String tenKM, int percent, String startDate, String endDate, AlertDialog dialog, Button btnSave) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            String errorMsg = "";

            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    PreparedStatement checkStmt = conn.prepareStatement("SELECT MaKM FROM KhuyenMai WHERE MaKM = ?");
                    checkStmt.setString(1, maKM);
                    if (checkStmt.executeQuery().next()) {
                        throw new Exception("Mã khuyến mãi này đã tồn tại!");
                    }
                    checkStmt.close();

                    String sql = "INSERT INTO KhuyenMai (MaKM, TenKM, PhanTramGiam, NgayBatDau, NgayKetThuc, TrangThai) VALUES (?, ?, ?, ?, ?, 1)";
                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setString(1, maKM);
                    pst.setString(2, tenKM);
                    pst.setInt(3, percent);
                    pst.setString(4, startDate);
                    pst.setString(5, endDate);

                    pst.executeUpdate();
                    pst.close(); conn.close();
                    isSuccess = true;
                } else {
                    errorMsg = "Lỗi kết nối máy chủ!";
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }

            final boolean finalSuccess = isSuccess;
            final String finalError = errorMsg;

            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "Thêm mã khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadPromoData();
                } else {
                    Toast.makeText(this, "Lỗi: " + finalError, Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("LƯU KHUYẾN MÃI");
                }
            });
        });
    }
}