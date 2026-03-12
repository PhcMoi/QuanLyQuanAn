package com.example.qlquanan;

import android.content.Intent;
import android.content.SharedPreferences; // Thêm import này
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast; // Thêm import này
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        MaterialCardView cardStaff = findViewById(R.id.cardStaff);
        MaterialCardView cardFood = findViewById(R.id.cardFood);
        MaterialCardView cardTable = findViewById(R.id.cardTable);
        MaterialCardView cardAnalytics = findViewById(R.id.cardAnalytics);
        MaterialCardView cardPromo = findViewById(R.id.cardPromo);

        Button btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);

        // 1. Quản lý Nhân sự
        cardStaff.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageStaffActivity.class);
            startActivity(intent);
        });

        // 2. Quản lý Món ăn
        cardFood.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageFoodActivity.class);
            startActivity(intent);
        });

        // 3. Quản lý Bàn
        cardTable.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageTableActivity.class);
            startActivity(intent);
        });

        // 4. Quản lý Khuyến Mãi
        cardPromo.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManagePromoActivity.class);
            startActivity(intent);
        });

        // 5. Thống kê nâng cao
        cardAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        // 6. Đăng xuất: ĐÃ SỬA LỖI XÓA SESSION
        btnLogoutAdmin.setOnClickListener(v -> {
            // --- BƯỚC QUAN TRỌNG: XÓA TRẠNG THÁI ĐĂNG NHẬP ---
            SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("IS_LOGGED_IN", false); // Đặt lại thành false
            editor.remove("USER_ROLE"); // Xóa vai trò
            editor.apply(); // Lưu thay đổi

            // Sau khi xóa xong mới quay về Login
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}