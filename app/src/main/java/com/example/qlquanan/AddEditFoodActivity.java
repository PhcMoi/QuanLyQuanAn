package com.example.qlquanan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditFoodActivity extends AppCompatActivity {

    private ImageView imgSelectedFood;
    private EditText edtFoodName, edtFoodPrice;
    private Spinner spinnerCategory;
    private Switch switchTrangThai;
    private Button btnSaveFood;
    private TextView tvTitleAddEditFood;

    private byte[] imageByteArray = null;
    private List<CategoryModel> categoryList = new ArrayList<>();
    private ArrayAdapter<CategoryModel> spinnerAdapter;

    private boolean isEditMode = false;
    private String editMaMon = "";
    private String editMaLoai = "";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        imgSelectedFood.setImageURI(imageUri);
                        imageByteArray = compressImageToBytes(imageUri);
                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi đọc ảnh!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_food);

        imgSelectedFood = findViewById(R.id.imgSelectedFood);
        edtFoodName = findViewById(R.id.edtFoodName);
        edtFoodPrice = findViewById(R.id.edtFoodPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchTrangThai = findViewById(R.id.switchTrangThai);
        btnSaveFood = findViewById(R.id.btnSaveFood);
        tvTitleAddEditFood = findViewById(R.id.tvTitleAddEditFood);

        findViewById(R.id.btnBackManageFood).setOnClickListener(v -> finish());

        switchTrangThai.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchTrangThai.setText(isChecked ? "Trạng thái: Đang bán" : "Trạng thái: Tạm ngưng (Hết hàng)");
        });

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryList);
        spinnerCategory.setAdapter(spinnerAdapter);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("MA_MON")) {
            isEditMode = true;
            editMaMon = intent.getStringExtra("MA_MON");
            editMaLoai = intent.getStringExtra("MA_LOAI");

            tvTitleAddEditFood.setText("Cập Nhật Món Ăn");
            btnSaveFood.setText("CẬP NHẬT MÓN ĂN");

            edtFoodName.setText(intent.getStringExtra("TEN_MON"));
            edtFoodPrice.setText(String.valueOf(intent.getIntExtra("GIA", 0)));

            int trangThaiCu = intent.getIntExtra("TRANG_THAI", 1);
            switchTrangThai.setChecked(trangThaiCu == 1);

            byte[] hinhAnhCu = intent.getByteArrayExtra("HINH_ANH");
            if (hinhAnhCu != null && hinhAnhCu.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(hinhAnhCu, 0, hinhAnhCu.length);
                imgSelectedFood.setImageBitmap(bitmap);
            }
        }

        loadCategoriesFromSQL();

        // --- ĐÃ SỬA LỖI CHỌN ẢNH Ở ĐÂY ---
        imgSelectedFood.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT); // Mở File Manager tổng hợp
            pickIntent.setType("image/*"); // Chỉ lọc lấy hình ảnh
            pickIntent.addCategory(Intent.CATEGORY_OPENABLE); // Mở rộng quyền đọc file từ mọi nguồn

            // Dùng launcher hiện tại để phóng hộp thoại Chooser xịn sò
            imagePickerLauncher.launch(Intent.createChooser(pickIntent, "Chọn ảnh món ăn từ..."));
        });

        btnSaveFood.setOnClickListener(v -> {
            String name = edtFoodName.getText().toString().trim();
            String priceStr = edtFoodPrice.getText().toString().trim();
            CategoryModel selectedCategory = (CategoryModel) spinnerCategory.getSelectedItem();
            int finalTrangThai = switchTrangThai.isChecked() ? 1 : 0;

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và giá món!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedCategory == null) {
                Toast.makeText(this, "Chưa tải được Loại Món!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEditMode && imageByteArray == null) {
                Toast.makeText(this, "Bạn phải chọn hình ảnh cho món mới!", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            saveFoodToDatabase(name, price, selectedCategory.getMaLoai(), imageByteArray, finalTrangThai);
        });
    }

    private void loadCategoriesFromSQL() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<CategoryModel> tempList = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    String query = "SELECT MaLoai, TenLoai FROM LoaiMon";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) tempList.add(new CategoryModel(rs.getString("MaLoai"), rs.getString("TenLoai")));
                    rs.close(); stmt.close(); conn.close();
                }
            } catch (Exception e) {}
            handler.post(() -> {
                if (!tempList.isEmpty()) {
                    categoryList.clear(); categoryList.addAll(tempList);
                    spinnerAdapter.notifyDataSetChanged();
                    if (isEditMode && editMaLoai != null) {
                        for (int i = 0; i < categoryList.size(); i++) {
                            if (categoryList.get(i).getMaLoai().equals(editMaLoai)) {
                                spinnerCategory.setSelection(i); break;
                            }
                        }
                    }
                }
            });
        });
    }

    private void saveFoodToDatabase(String name, double price, String maLoai, byte[] imageBytes, int trangThai) {
        btnSaveFood.setEnabled(false);
        btnSaveFood.setText("Đang xử lý...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean isSuccess = false;
            String errorMessage = "";
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    PreparedStatement stmt = null;
                    if (isEditMode) {
                        if (imageBytes != null) {
                            String sqlUpdate = "UPDATE MonAn SET TenMon = ?, DonGia = ?, MaLoai = ?, HinhAnh = ?, TrangThai = ? WHERE MaMon = ?";
                            stmt = conn.prepareStatement(sqlUpdate);
                            stmt.setString(1, name); stmt.setDouble(2, price); stmt.setString(3, maLoai); stmt.setBytes(4, imageBytes); stmt.setInt(5, trangThai); stmt.setString(6, editMaMon);
                        } else {
                            String sqlUpdateNoImg = "UPDATE MonAn SET TenMon = ?, DonGia = ?, MaLoai = ?, TrangThai = ? WHERE MaMon = ?";
                            stmt = conn.prepareStatement(sqlUpdateNoImg);
                            stmt.setString(1, name); stmt.setDouble(2, price); stmt.setString(3, maLoai); stmt.setInt(4, trangThai); stmt.setString(5, editMaMon);
                        }
                    } else {
                        String sqlInsert = "INSERT INTO MonAn (TenMon, DonGia, TrangThai, MaLoai, HinhAnh) VALUES (?, ?, ?, ?, ?)";
                        stmt = conn.prepareStatement(sqlInsert);
                        stmt.setString(1, name); stmt.setDouble(2, price); stmt.setInt(3, trangThai); stmt.setString(4, maLoai); stmt.setBytes(5, imageBytes);
                    }
                    if (stmt != null) {
                        stmt.executeUpdate(); stmt.close();
                    }
                    conn.close();
                    isSuccess = true;
                }
            } catch (Throwable e) { errorMessage = e.toString(); }

            final boolean finalSuccess = isSuccess;
            final String finalError = errorMessage;
            runOnUiThread(() -> {
                btnSaveFood.setEnabled(true);
                btnSaveFood.setText(isEditMode ? "CẬP NHẬT MÓN ĂN" : "LƯU MÓN ĂN");
                if (finalSuccess) {
                    new androidx.appcompat.app.AlertDialog.Builder(AddEditFoodActivity.this)
                            .setTitle("🎉 THÀNH CÔNG")
                            .setMessage("Cập nhật dữ liệu thành công!")
                            .setPositiveButton("Tuyệt vời", (dialog, which) -> finish())
                            .setCancelable(false).show();
                } else {
                    Toast.makeText(this, "Lỗi SQL: " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private byte[] compressImageToBytes(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        float ratio = Math.min((float) 800 / bitmap.getWidth(), (float) 800 / bitmap.getHeight());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, Math.round(ratio * bitmap.getWidth()), Math.round(ratio * bitmap.getHeight()), true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }
}