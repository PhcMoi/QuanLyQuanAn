package com.example.qlquanan;

import android.content.Intent;
import android.content.SharedPreferences; // THÊM DÒNG NÀY
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class OrderActivity extends AppCompatActivity {

    public static ArrayList<OrderItem> orderList = new ArrayList<>();
    private OrderSummaryAdapter orderAdapter;
    private TextView tvSubtotalValue;
    private double currentTotal = 0;
    private Button btnProceed;
    private String tableId;
    private String maHDHienTai = ""; // BIẾN LƯU MÃ HÓA ĐƠN

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // --- SỬA LỖI 2: DỌN SẠCH GIỎ HÀNG CŨ TRƯỚC KHI VÀO BÀN MỚI ---
        orderList.clear();

        String tableName = getIntent().getStringExtra("TABLE_NAME");
        tableId = getIntent().getStringExtra("TABLE_ID");
        boolean isOccupied = getIntent().getBooleanExtra("IS_OCCUPIED", false);

        TextView tvTableTitle = findViewById(R.id.tvTableTitle);
        TextView tvTableBadge = findViewById(R.id.tvTableBadge);
        tvSubtotalValue = findViewById(R.id.tvSubtotalValue);

        tvTableTitle.setText(tableName != null ? tableName : "Bàn");

        GradientDrawable badgeShape = new GradientDrawable();
        badgeShape.setCornerRadius(20f);
        if (isOccupied) {
            tvTableBadge.setText("Đang có khách");
            badgeShape.setColor(Color.parseColor("#FF9800"));
        } else {
            tvTableBadge.setText("Trống");
            badgeShape.setColor(Color.parseColor("#4CAF50"));
        }
        tvTableBadge.setBackground(badgeShape);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvFood = findViewById(R.id.rvFood);
        rvFood.setLayoutManager(new GridLayoutManager(this, 2));
        List<FoodModel> foodList = new ArrayList<>();
        FoodAdapter foodAdapter = new FoodAdapter(foodList, this::addToOrder, null);
        rvFood.setAdapter(foodAdapter);

        RecyclerView rvOrderSummary = findViewById(R.id.rvOrderSummary);
        rvOrderSummary.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderSummaryAdapter(orderList, new OrderSummaryAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(OrderItem item) {
                item.setQuantity(item.getQuantity() + 1);
                updateSubtotal();
            }
            @Override
            public void onDecrease(OrderItem item) {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                } else {
                    orderList.remove(item);
                }
                updateSubtotal();
            }
        });
        rvOrderSummary.setAdapter(orderAdapter);

        loadFoodMenu(foodAdapter, foodList);

        // Nếu bàn có khách, tải hóa đơn hiện tại lên
        if (tableId != null && !tableId.isEmpty() && isOccupied) {
            loadExistingOrder(tableId);
        }

        Button btnViewBill = findViewById(R.id.btnViewBill);
        btnProceed = findViewById(R.id.btnProceed);

        // --- SỬA LỖI 1: GỬI KÈM MÃ HÓA ĐƠN SANG BILL DETAIL ---
        btnViewBill.setOnClickListener(v -> {
            if (orderList.isEmpty()) {
                Toast.makeText(this, "Chưa có món nào để xem bill!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (maHDHienTai.isEmpty()) {
                Toast.makeText(this, "Vui lòng bấm 'GỬI BẾP' để chốt đơn trước khi xem Bill!", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(this, BillDetailActivity.class);
            intent.putExtra("TOTAL_AMOUNT", currentTotal);
            intent.putExtra("TABLE_ID", tableId);
            intent.putExtra("MA_HD", maHDHienTai); // CHÌA KHÓA QUAN TRỌNG
            startActivity(intent);
        });

        btnProceed.setOnClickListener(v -> {
            if (orderList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn món trước khi gửi bếp!", Toast.LENGTH_SHORT).show();
                return;
            }
            processOrderTransaction(tableId);
        });
    }

    private void loadFoodMenu(FoodAdapter foodAdapter, List<FoodModel> foodList) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    String query = "SELECT m.MaMon, m.TenMon, m.HinhAnh, m.DonGia AS GiaGoc, " +
                            "ISNULL(k.PhanTramGiam, 0) AS PhanTramGiam, " +
                            "CAST(CASE " +
                            "   WHEN k.MaKM IS NOT NULL THEN m.DonGia - (m.DonGia * k.PhanTramGiam / 100.0) " +
                            "   ELSE m.DonGia " +
                            "END AS INT) AS GiaBan " +
                            "FROM MonAn m " +
                            "LEFT JOIN KhuyenMai k ON m.MaKM = k.MaKM AND k.TrangThai = 1 AND CAST(GETDATE() AS DATE) BETWEEN k.NgayBatDau AND k.NgayKetThuc " +
                            "WHERE m.TrangThai = 1";

                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    List<FoodModel> listTuDB = new ArrayList<>();

                    while (rs.next()) {
                        listTuDB.add(new FoodModel(
                                rs.getString("MaMon"),
                                rs.getString("TenMon"),
                                rs.getInt("GiaBan"),
                                "",
                                rs.getBytes("HinhAnh"),
                                1,
                                rs.getInt("GiaGoc"),
                                rs.getInt("PhanTramGiam")));
                    }
                    rs.close(); stmt.close(); conn.close();

                    handler.post(() -> {
                        foodList.clear();
                        foodList.addAll(listTuDB);
                        foodAdapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e("SQL_FOOD", "Lỗi tải menu: " + e.getMessage());
            }
        });
    }

    private void loadExistingOrder(String maBan) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // Lấy MaHD đang mở cùng với chi tiết món
                    String query = "SELECT d.MaHD, m.MaMon, m.TenMon, m.HinhAnh, c.SoLuong, c.DonGia AS GiaBan " +
                            "FROM ChiTietHoaDon c " +
                            "INNER JOIN HoaDon d ON c.MaHD = d.MaHD " +
                            "INNER JOIN MonAn m ON c.MaMon = m.MaMon " +
                            "WHERE d.MaBan = ? AND d.ThoiGianDong IS NULL";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, maBan);
                    ResultSet rs = stmt.executeQuery();
                    List<OrderItem> loadedItems = new ArrayList<>();

                    while (rs.next()) {
                        maHDHienTai = rs.getString("MaHD"); // LƯU LẠI MÃ HÓA ĐƠN ĐỂ GỬI SANG BILL
                        FoodModel food = new FoodModel(
                                rs.getString("MaMon"),
                                rs.getString("TenMon"),
                                rs.getInt("GiaBan"),
                                "",
                                rs.getBytes("HinhAnh"),
                                1,
                                rs.getInt("GiaBan"),
                                0);
                        loadedItems.add(new OrderItem(food, rs.getInt("SoLuong")));
                    }
                    rs.close(); stmt.close(); conn.close();

                    handler.post(() -> {
                        if (!loadedItems.isEmpty()) {
                            orderList.clear();
                            orderList.addAll(loadedItems);
                            if (orderAdapter != null) orderAdapter.notifyDataSetChanged();
                            updateSubtotal();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("SQL_LOAD_ORDER", "Lỗi kéo đơn: " + e.getMessage());
            }
        });
    }

    private void processOrderTransaction(String maBan) {
        btnProceed.setEnabled(false);
        btnProceed.setText("Đang gửi...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // LẤY ID NHÂN VIÊN ĐANG ĐĂNG NHẬP
        SharedPreferences pref = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        String currentNV = pref.getString("USER_ID", "NV0001"); // Mặc định NV0001 nếu lỗi

        executor.execute(() -> {
            boolean isSuccess = false;
            String error = "";
            Connection conn = null;
            try {
                conn = new SQLConnection().connection();
                if (conn != null) {
                    conn.setAutoCommit(false);

                    PreparedStatement ps = conn.prepareStatement("SELECT MaHD FROM HoaDon WHERE MaBan = ? AND ThoiGianDong IS NULL");
                    ps.setString(1, maBan);
                    ResultSet rs = ps.executeQuery();
                    String maDonHienTai = rs.next() ? rs.getString("MaHD") : null;
                    ps.close();

                    if (maDonHienTai == null) {
                        // --- SỬA LỖI 3: DÙNG ID NHÂN VIÊN THẬT THAY VÌ HARDCODE ---
                        PreparedStatement psIns = conn.prepareStatement(
                                "INSERT INTO HoaDon (MaBan, NgayLap, ThoiGianMo, MaNV) VALUES (?, CAST(GETDATE() AS DATE), CAST(GETDATE() AS TIME), ?)"
                        );
                        psIns.setString(1, maBan);
                        psIns.setString(2, currentNV);
                        psIns.executeUpdate();
                        psIns.close();

                        PreparedStatement psGet = conn.prepareStatement("SELECT MaHD FROM HoaDon WHERE MaBan = ? AND ThoiGianDong IS NULL");
                        psGet.setString(1, maBan);
                        ResultSet rsGet = psGet.executeQuery();
                        if (rsGet.next()) {
                            maDonHienTai = rsGet.getString("MaHD");
                            maHDHienTai = maDonHienTai; // CẬP NHẬT BIẾN TOÀN CỤC
                        } else {
                            throw new Exception("Lỗi: Không lấy được MaHD sau khi tạo!");
                        }
                        rsGet.close(); psGet.close();

                        PreparedStatement psUpBan = conn.prepareStatement("UPDATE Ban SET TrangThai = 1 WHERE MaBan = ?");
                        psUpBan.setString(1, maBan);
                        psUpBan.executeUpdate();
                        psUpBan.close();
                    } else {
                        maHDHienTai = maDonHienTai; // NẾU ĐÃ CÓ HÓA ĐƠN THÌ CŨNG PHẢI LƯU LẠI
                    }

                    PreparedStatement psDel = conn.prepareStatement("DELETE FROM ChiTietHoaDon WHERE MaHD = ?");
                    psDel.setString(1, maDonHienTai);
                    psDel.executeUpdate();
                    psDel.close();

                    PreparedStatement psInsCt = conn.prepareStatement("INSERT INTO ChiTietHoaDon (MaHD, MaMon, SoLuong, DonGia) VALUES (?, ?, ?, ?)");
                    for (OrderItem item : orderList) {
                        psInsCt.setString(1, maDonHienTai);
                        psInsCt.setString(2, item.getFoodItemActivity().getMaMon());
                        psInsCt.setInt(3, item.getQuantity());
                        psInsCt.setInt(4, item.getFoodItemActivity().getGia());
                        psInsCt.executeUpdate();
                    }
                    psInsCt.close();

                    // CẬP NHẬT TỔNG TIỀN VÀO BẢNG HÓA ĐƠN ĐỂ TIỆN THỐNG KÊ SAU NÀY
                    PreparedStatement psTongTien = conn.prepareStatement("UPDATE HoaDon SET TongTien = ? WHERE MaHD = ?");
                    psTongTien.setDouble(1, currentTotal);
                    psTongTien.setString(2, maDonHienTai);
                    psTongTien.executeUpdate();
                    psTongTien.close();

                    conn.commit();
                    isSuccess = true;
                }
            } catch (Exception e) {
                error = e.getMessage();
                if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
            } finally {
                if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) {}
            }

            final boolean finalSuccess = isSuccess;
            final String finalError = error;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "Chốt đơn thành công! Hãy xem Bill.", Toast.LENGTH_SHORT).show();
                    // KHÔNG DÙNG finish() Ở ĐÂY NỮA, ĐỂ NHÂN VIÊN BẤM XEM BILL
                    btnProceed.setEnabled(true);
                    btnProceed.setText("Gửi bếp / Cập nhật");
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("🚨 Lỗi SQL Chi Tiết")
                            .setMessage(finalError)
                            .setPositiveButton("Đóng", null)
                            .show();
                    btnProceed.setEnabled(true);
                    btnProceed.setText("Gửi bếp");
                }
            });
        });
    }

    private void addToOrder(FoodModel food) {
        boolean exists = false;
        for (OrderItem item : orderList) {
            if (item.getFoodItemActivity().getMaMon().equals(food.getMaMon())) {
                item.setQuantity(item.getQuantity() + 1);
                exists = true;
                break;
            }
        }
        if (!exists) orderList.add(new OrderItem(food, 1));
        updateSubtotal();
    }

    private void updateSubtotal() {
        currentTotal = 0;
        for (OrderItem item : orderList) {
            currentTotal += item.getFoodItemActivity().getGia() * item.getQuantity();
        }
        tvSubtotalValue.setText(String.format("%,.0f đ", currentTotal));
        if (orderAdapter != null) orderAdapter.notifyDataSetChanged();
    }
}