package com.example.qlquanan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageTableActivity extends AppCompatActivity {

    private RecyclerView rvAdminTables;
    private ManageTableAdapter adapter;
    private List<TableModel> tableListFull = new ArrayList<>();
    private List<TableModel> tableList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshAdminTable;
    private EditText edtSearchTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_table);

        findViewById(R.id.btnBackAdminTable).setOnClickListener(v -> finish());
        swipeRefreshAdminTable = findViewById(R.id.swipeRefreshAdminTable);
        edtSearchTable = findViewById(R.id.edtSearchTable);
        rvAdminTables = findViewById(R.id.rvAdminTables);

        rvAdminTables.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageTableAdapter(tableList, new ManageTableAdapter.OnTableAction() {
            @Override
            public void onEdit(TableModel table) {
                showAddEditDialog(table); // Bấm sửa
            }

            @Override
            public void onDelete(TableModel table) {
                confirmDelete(table); // Bấm xóa
            }
        });
        rvAdminTables.setAdapter(adapter);

        findViewById(R.id.btnAddTableHeader).setOnClickListener(v -> showAddEditDialog(null));

        swipeRefreshAdminTable.setOnRefreshListener(this::loadTables);

        edtSearchTable.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTables(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTables();
    }

    private void loadTables() {
        swipeRefreshAdminTable.setRefreshing(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<TableModel> temp = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT MaBan, TenBan, TrangThai FROM Ban");
                    while (rs.next()) {
                        String maBan = rs.getString("MaBan");
                        if (maBan != null) maBan = maBan.trim();
                        temp.add(new TableModel(maBan, rs.getString("TenBan"), rs.getInt("TrangThai")));
                    }
                    conn.close();
                }
            } catch (Exception e) {
                Log.e("SQL_TABLE", "Lỗi tải bàn: " + e.getMessage());
            }

            handler.post(() -> {
                tableListFull.clear();
                tableListFull.addAll(temp);
                filterTables(edtSearchTable.getText().toString());
                swipeRefreshAdminTable.setRefreshing(false);
            });
        });
    }

    private void filterTables(String query) {
        tableList.clear();
        if (query.trim().isEmpty()) {
            tableList.addAll(tableListFull);
        } else {
            String q = query.toLowerCase();
            for (TableModel t : tableListFull) {
                if (t.getTenBan().toLowerCase().contains(q) || t.getMaBan().toLowerCase().contains(q)) {
                    tableList.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- FORM THÊM / SỬA BÀN DÙNG CHUNG ---
    private void showAddEditDialog(TableModel table) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(table == null ? "✨ Thêm Bàn Mới" : "✏️ Cập Nhật Tên Bàn");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Ô nhập Mã bàn đã bị ẩn đối với lúc Thêm mới. Chỉ hiện khi đang Sửa để Admin xem.
        final EditText edtMaBan = new EditText(this);
        if (table != null) {
            edtMaBan.setText("Mã hệ thống: " + table.getMaBan());
            edtMaBan.setEnabled(false); // Chỉ đọc
            layout.addView(edtMaBan);
        }

        final EditText edtTenBan = new EditText(this);
        edtTenBan.setHint("Nhập tên bàn (VD: Bàn VIP 1)");
        if (table != null) edtTenBan.setText(table.getTenBan());
        layout.addView(edtTenBan);

        builder.setView(layout);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String ten = edtTenBan.getText().toString().trim();
            if (ten.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên bàn!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Nếu là sửa thì truyền mã cũ vào, nếu là thêm thì truyền rỗng (SQL sẽ tự sinh)
            String ma = (table != null) ? table.getMaBan() : "";
            executeSaveTable(ma, ten, table != null);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void executeSaveTable(String maBan, String tenBan, boolean isEditMode) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean success = false;
            String msg = "";
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    PreparedStatement stmt;
                    if (isEditMode) {
                        // CHẾ ĐỘ SỬA
                        stmt = conn.prepareStatement("UPDATE Ban SET TenBan = ? WHERE RTRIM(MaBan) = ?");
                        stmt.setString(1, tenBan);
                        stmt.setString(2, maBan);
                        stmt.executeUpdate();
                        stmt.close();
                    } else {
                        // CHẾ ĐỘ THÊM: Tự động đếm BA0001, BA0002...
                        String sqlInsert =
                                "DECLARE @NextID INT; " +
                                        "SELECT @NextID = ISNULL(MAX(CAST(SUBSTRING(MaBan, 3, 10) AS INT)), 0) + 1 " +
                                        "FROM Ban WHERE MaBan LIKE 'BA%'; " +
                                        "DECLARE @NewMaBan VARCHAR(20) = 'BA' + RIGHT('0000' + CAST(@NextID AS VARCHAR), 4); " +
                                        "INSERT INTO Ban (MaBan, TenBan, TrangThai) VALUES (@NewMaBan, ?, 0);";

                        stmt = conn.prepareStatement(sqlInsert);
                        stmt.setString(1, tenBan);
                        stmt.executeUpdate();
                        stmt.close();
                    }
                    conn.close();
                    success = true;
                }
            } catch (Exception e) {
                msg = e.getMessage();
                Log.e("SQL_ADD_TABLE", msg);
            }

            final boolean finalSuccess = success;
            final String finalMsg = msg;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, isEditMode ? "Đã cập nhật!" : "Đã tạo bàn mới thành công!", Toast.LENGTH_SHORT).show();
                    loadTables(); // Load lại để hiển thị mã BAxxxx mới
                } else {
                    new AlertDialog.Builder(this).setTitle("Lỗi SQL").setMessage(finalMsg).show();
                }
            });
        });
    }

    // --- XÓA BÀN BỌC THÉP ---
    private void confirmDelete(TableModel table) {
        if (table.getTrangThai() == 1) {
            new AlertDialog.Builder(this)
                    .setTitle("🚨 CẢNH BÁO KHIẾM KHUYẾT")
                    .setMessage("Bàn '" + table.getTenBan() + "' đang có khách ngồi. Hệ thống từ chối xóa để bảo vệ hóa đơn!")
                    .setPositiveButton("Đã hiểu", null).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa")
                .setMessage("Bạn có chắc muốn vứt bỏ '" + table.getTenBan() + "' khỏi hệ thống?")
                .setPositiveButton("Xóa ngay", (dialog, which) -> executeDelete(table.getMaBan()))
                .setNegativeButton("Hủy", null).show();
    }

    private void executeDelete(String maBan) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean success = false;
            String msg = "";
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM Ban WHERE RTRIM(MaBan) = ?");
                    stmt.setString(1, maBan);
                    stmt.executeUpdate(); stmt.close(); conn.close();
                    success = true;
                }
            } catch (Exception e) {
                msg = e.getMessage();
                if (msg.contains("REFERENCE constraint")) {
                    msg = "Không thể xóa cứng bàn này vì nó đang chứa lịch sử hóa đơn cũ. Vui lòng giữ lại để đảm bảo dữ liệu Kế toán!";
                }
            }

            final boolean finalSuccess = success;
            final String finalMsg = msg;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "Đã dẹp bàn thành công!", Toast.LENGTH_SHORT).show();
                    loadTables();
                } else {
                    new AlertDialog.Builder(this).setTitle("Lỗi Xóa Bàn").setMessage(finalMsg).show();
                }
            });
        });
    }
}