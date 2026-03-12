package com.example.qlquanan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageFoodActivity extends AppCompatActivity {

    private RecyclerView rvFoodListAdmin;
    private FoodAdapter foodAdapter;
    private List<FoodModel> foodList;
    private List<FoodModel> foodListFull;
    private EditText edtSearchFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_food);

        findViewById(R.id.btnBackAdminFood).setOnClickListener(v -> finish());

        ImageButton btnAddFoodTop = findViewById(R.id.btnAddFoodTop);
        btnAddFoodTop.setOnClickListener(v -> {
            Intent intent = new Intent(ManageFoodActivity.this, AddEditFoodActivity.class);
            startActivity(intent);
        });

        rvFoodListAdmin = findViewById(R.id.rvFoodListAdmin);
        rvFoodListAdmin.setLayoutManager(new GridLayoutManager(this, 2));

        foodList = new ArrayList<>();
        foodListFull = new ArrayList<>();

        foodAdapter = new FoodAdapter(foodList,
                food -> {
                    Toast.makeText(ManageFoodActivity.this, "Nhấn giữ (Long Click) để Sửa/Xóa nhé!", Toast.LENGTH_SHORT).show();
                },
                food -> showOptionsDialog(food)
        );
        rvFoodListAdmin.setAdapter(foodAdapter);

        edtSearchFood = findViewById(R.id.edtSearchFood);
        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFoodList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFoodListFromSQL();
    }

    private void loadFoodListFromSQL() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<FoodModel> listTuDB = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // Kéo cả những món đang bán (1) và tạm ngưng (0), bỏ qua món đã xóa (-1)
                    String query = "SELECT MaMon, TenMon, DonGia, MaLoai, HinhAnh, TrangThai FROM MonAn WHERE TrangThai >= 0";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);

                    while (rs.next()) {
                        String maMon = rs.getString("MaMon");
                        // TUYỆT KỸ GỌT KHOẢNG TRẮNG (Phòng hờ kiểu CHAR trong SQL)
                        if (maMon != null) maMon = maMon.trim();

                        String tenMon = rs.getString("TenMon");
                        double gia = rs.getDouble("DonGia");
                        String maLoai = rs.getString("MaLoai");
                        byte[] hinhAnh = rs.getBytes("HinhAnh");
                        int trangThai = rs.getInt("TrangThai");

                        listTuDB.add(new FoodModel(maMon, tenMon, (int) gia, maLoai, hinhAnh, trangThai, 0, 0));
                    }
                    rs.close(); stmt.close(); conn.close();
                }
            } catch (Exception e) {
                Log.e("SQL_MANAGE_FOOD", "Lỗi kéo thực đơn: " + e.getMessage());
            }

            handler.post(() -> {
                foodListFull.clear();
                foodListFull.addAll(listTuDB);

                String currentSearchText = edtSearchFood.getText().toString().trim();
                if (!currentSearchText.isEmpty()) {
                    filterFoodList(currentSearchText);
                } else {
                    foodList.clear();
                    foodList.addAll(listTuDB);
                    foodAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showOptionsDialog(FoodModel food) {
        String[] options = {"✏️ Sửa thông tin món", "🗑️ Xóa món này"};

        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn: " + food.getTenMon())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(ManageFoodActivity.this, AddEditFoodActivity.class);
                        intent.putExtra("MA_MON", food.getMaMon());
                        intent.putExtra("TEN_MON", food.getTenMon());
                        intent.putExtra("GIA", food.getGia());
                        intent.putExtra("MA_LOAI", food.getMaLoai());
                        intent.putExtra("HINH_ANH", food.getHinhAnh());

                        // SỬA LỖI 2: Đóng gói thêm Trạng Thái để form Sửa biết đường gạt công tắc
                        intent.putExtra("TRANG_THAI", food.getTrangThai());

                        startActivity(intent);
                    } else if (which == 1) {
                        confirmDeleteFood(food);
                    }
                })
                .show();
    }

    private void confirmDeleteFood(FoodModel food) {
        new AlertDialog.Builder(this)
                .setTitle("Cảnh báo Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa món '" + food.getTenMon() + "' khỏi thực đơn?")
                .setPositiveButton("Xóa ngay", (dialog, which) -> executeSoftDelete(food.getMaMon()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeSoftDelete(String maMon) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            String errorMessage = "";
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // TUYỆT KỸ CHÉM KHOẢNG TRẮNG BẰNG LỆNH LTRIM/RTRIM CỦA SQL
                    String sqlUpdate = "UPDATE MonAn SET TrangThai = -1 WHERE RTRIM(LTRIM(MaMon)) = RTRIM(LTRIM(?))";
                    PreparedStatement stmt = conn.prepareStatement(sqlUpdate);
                    stmt.setString(1, maMon);

                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        isSuccess = true;
                    } else {
                        errorMessage = "CẢNH BÁO: SQL không tìm thấy món nào có mã là [" + maMon + "] để xóa!";
                    }

                    stmt.close(); conn.close();
                } else {
                    errorMessage = "Lỗi đường truyền: Không kết nối được SQL Server!";
                }
            } catch (Exception e) {
                errorMessage = "Bị Lỗi Cú Pháp SQL:\n" + e.getMessage();
                Log.e("SQL_DELETE_FOOD", "Lỗi: " + errorMessage);
            }

            final boolean finalSuccess = isSuccess;
            final String finalError = errorMessage;

            // DÙNG BẢNG KHỔNG LỒ THAY VÌ TOAST ĐỂ BẮT BUỘC PHẢI ĐỌC
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(ManageFoodActivity.this, "ĐÃ CHÉM BAY MÓN ĂN!", Toast.LENGTH_SHORT).show();
                    loadFoodListFromSQL(); // Load lại danh sách
                } else {
                    new AlertDialog.Builder(ManageFoodActivity.this)
                            .setTitle("🚨 PHÁT HIỆN ĐIỂM MÙ")
                            .setMessage(finalError)
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                }
            });
        });
    }


    private void filterFoodList(String text) {
        foodList.clear();
        if (text.isEmpty()) {
            foodList.addAll(foodListFull);
        } else {
            text = text.toLowerCase();
            for (FoodModel item : foodListFull) {
                if (item.getTenMon().toLowerCase().contains(text) || item.getMaMon().toLowerCase().contains(text)) {
                    foodList.add(item);
                }
            }
        }
        foodAdapter.notifyDataSetChanged();
    }
}