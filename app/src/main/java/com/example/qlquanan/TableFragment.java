package com.example.qlquanan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class TableFragment extends Fragment {

    private RecyclerView rvTables;
    private TableAdapter adapter;
    private List<TableModel> tableList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvAvailable, tvOccupied;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Nạp giao diện fragment_table.xml
        View view = inflater.inflate(R.layout.fragment_table, container, false);

        // 2. Ánh xạ View (Phải dùng view.findViewById)
        rvTables = view.findViewById(R.id.rvTables);
        tvAvailable = view.findViewById(R.id.tvAvailable);
        tvOccupied = view.findViewById(R.id.tvOccupied);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // 3. Cấu hình RecyclerView
        tableList = new ArrayList<>();
        rvTables.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new TableAdapter(tableList,
                table -> { // Click nhẹ
                    Intent intent = new Intent(getActivity(), OrderActivity.class);
                    intent.putExtra("TABLE_ID", table.getMaBan());
                    intent.putExtra("TABLE_NAME", table.getTenBan());
                    intent.putExtra("IS_OCCUPIED", table.getTrangThai() == 1);
                    startActivity(intent);
                },
                table -> { // Long click
                    if (table.getTrangThai() == 0) {
                        Toast.makeText(getContext(), "Bàn trống!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showTransferDialog(table);
                }
        );
        rvTables.setAdapter(adapter);

        // 4. Swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadTables);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTables();
    }

    private void loadTables() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<TableModel> tempTableList = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    String query = "SELECT MaBan, TenBan, TrangThai FROM Ban";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        tempTableList.add(new TableModel(rs.getString("MaBan"), rs.getString("TenBan"), rs.getInt("TrangThai")));
                    }
                    rs.close(); stmt.close(); conn.close();
                }
            } catch (Exception e) {
                Log.e("SQL_ERROR", "Lỗi tải bàn: " + e.getMessage());
            }

            handler.post(() -> {
                tableList.clear();
                tableList.addAll(tempTableList);
                adapter.notifyDataSetChanged();
                updateStatus();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            });
        });
    }

    private void updateStatus() {
        int available = 0, occupied = 0;
        for (TableModel t : tableList) {
            if (t.getTrangThai() == 1) occupied++; else available++;
        }
        if (tvAvailable != null) tvAvailable.setText(available + " Trống");
        if (tvOccupied != null) tvOccupied.setText(occupied + " Có Khách");
    }

    // --- Giữ nguyên logic showTransferDialog và executeTransferMergeTransaction từ file cũ của bạn ---
    // Lưu ý: Những chỗ gọi "TableActivity.this" hãy đổi thành "getActivity()" hoặc "getContext()"
    private void showTransferDialog(TableModel sourceTable) {
        List<String> options = new ArrayList<>();
        List<TableModel> targetTables = new ArrayList<>();
        for (TableModel t : tableList) {
            if (!t.getMaBan().equals(sourceTable.getMaBan())) {
                targetTables.add(t);
                options.add(t.getTenBan() + (t.getTrangThai() == 1 ? " (Gộp)" : " (Chuyển)"));
            }
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Chuyển bàn: " + sourceTable.getTenBan())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    confirmTransfer(sourceTable, targetTables.get(which));
                }).show();
    }

    private void confirmTransfer(TableModel source, TableModel target) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận")
                .setMessage("Thực hiện thao tác với " + target.getTenBan() + "?")
                .setPositiveButton("Đồng ý", (d, w) -> executeTransferMergeTransaction(source, target))
                .setNegativeButton("Hủy", null).show();
    }

    private void executeTransferMergeTransaction(TableModel source, TableModel target) {
        // ... (Dán nốt đoạn logic Transaction xịn xò của bạn vào đây)
        // Nhớ đổi Toast.makeText(TableActivity.this, ...) thành Toast.makeText(getContext(), ...)
    }
}