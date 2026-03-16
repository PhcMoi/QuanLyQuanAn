package com.example.qlquanan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
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

public class AssignPromoActivity extends AppCompatActivity {

    private String currentMaKM; // Mã nhận được từ màn hình trước
    private RecyclerView rvAssignFoodList;
    private Button btnSaveAssignment;
    private AssignPromoAdapter adapter;
    private List<FoodAssignModel> foodList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_promo);

        // Lấy Mã KM từ Intent (Bạn nhớ putExtra từ màn hình ManagePromoActivity nhé)
        currentMaKM = getIntent().getStringExtra("MA_KM");
        if (currentMaKM == null) currentMaKM = "KM001"; // Test mặc định

        TextView tvTitle = findViewById(R.id.tvAssignTitle);
        tvTitle.setText("Gán Mã: " + currentMaKM);

        findViewById(R.id.btnBackAssign).setOnClickListener(v -> finish());
        btnSaveAssignment = findViewById(R.id.btnSaveAssignment);

        rvAssignFoodList = findViewById(R.id.rvAssignFoodList);
        rvAssignFoodList.setLayoutManager(new LinearLayoutManager(this));
        foodList = new ArrayList<>();

        adapter = new AssignPromoAdapter(foodList, currentMaKM, totalSelected -> {
            btnSaveAssignment.setText("LƯU CÀI ĐẶT (" + totalSelected + " MÓN)");
        });
        rvAssignFoodList.setAdapter(adapter);

        loadFoodToAssign();

        btnSaveAssignment.setOnClickListener(v -> saveBatchAssignment());
    }

    private void loadFoodToAssign() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            List<FoodAssignModel> temp = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // LƯU Ý: Thay DonGia bằng cột giá trong bảng MonAn của bạn
                    // Ép SQL kiểm tra chéo: Chỉ lấy MaKM lên NẾU Khuyến mãi đó chưa hết hạn và đang được kích hoạt
                    String sql = "SELECT m.MaMon, m.TenMon, m.DonGia, k.MaKM " +
                            "FROM MonAn m " +
                            "LEFT JOIN KhuyenMai k ON m.MaKM = k.MaKM " +
                            "AND k.NgayKetThuc >= CAST(GETDATE() AS DATE) " +
                            "AND k.TrangThai = 1 " +
                            "WHERE m.TrangThai = 1";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    while (rs.next()) {
                        String maKMDangCo = rs.getString("MaKM");
                        // Nếu món ăn đang giữ đúng cái mã KM này -> Đánh dấu Check sẵn
                        boolean isChecked = (maKMDangCo != null && maKMDangCo.equalsIgnoreCase(currentMaKM));

                        temp.add(new FoodAssignModel(
                                rs.getString("MaMon"),
                                rs.getString("TenMon"),
                                rs.getDouble("DonGia"),
                                maKMDangCo,
                                isChecked
                        ));
                    }
                    rs.close(); stmt.close(); conn.close();
                    isSuccess = true;
                }
            } catch (Exception e) {
                Log.e("SQL_ASSIGN", e.getMessage());
            }

            final boolean fSuccess = isSuccess;
            handler.post(() -> {
                if (fSuccess) {
                    foodList.clear(); foodList.addAll(temp);
                    adapter.notifyDataSetChanged();

                    // Cập nhật số lượng đếm ban đầu
                    int count = 0;
                    for (FoodAssignModel f : foodList) if (f.isSelected()) count++;
                    btnSaveAssignment.setText("LƯU CÀI ĐẶT (" + count + " MÓN)");
                } else {
                    Toast.makeText(this, "Lỗi tải thực đơn", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveBatchAssignment() {
        btnSaveAssignment.setEnabled(false);
        btnSaveAssignment.setText("Đang xử lý...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false; String error = ""; Connection conn = null;
            try {
                conn = new SQLConnection().connection();
                if (conn != null) {
                    conn.setAutoCommit(false); // Bật Transaction bảo vệ

                    // Lệnh 1: Gán Mã KM cho các món ĐƯỢC CHECK
                    PreparedStatement pstUpdate = conn.prepareStatement("UPDATE MonAn SET MaKM = ? WHERE MaMon = ?");
                    // Lệnh 2: Xóa Mã KM (set NULL) cho các món KHÔNG CHECK (nhưng trước đó lỡ mang mã này)
                    PreparedStatement pstRemove = conn.prepareStatement("UPDATE MonAn SET MaKM = NULL WHERE MaMon = ?");

                    for (FoodAssignModel food : foodList) {
                        if (food.isSelected()) {
                            pstUpdate.setString(1, currentMaKM);
                            pstUpdate.setString(2, food.getMaMon());
                            pstUpdate.addBatch(); // Gom lệnh vào một gói (Batch)
                        } else if (food.getMaKMDangCo() != null && food.getMaKMDangCo().equalsIgnoreCase(currentMaKM)) {
                            // Bị uncheck -> Tháo gỡ mã khuyến mãi
                            pstRemove.setString(1, food.getMaMon());
                            pstRemove.addBatch();
                        }
                    }

                    // Thực thi 1 phát gửi toàn bộ lên Server
                    pstUpdate.executeBatch();
                    pstRemove.executeBatch();

                    pstUpdate.close(); pstRemove.close();
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
                if (fSuccess) {
                    Toast.makeText(this, "Áp dụng khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại màn hình quản lý
                } else {
                    btnSaveAssignment.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + fError, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}