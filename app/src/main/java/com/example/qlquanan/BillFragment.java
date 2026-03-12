package com.example.qlquanan;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BillFragment extends Fragment {

    private RecyclerView rvBills;
    private List<BillModel> billList; // Danh sách gốc từ SQL
    private List<BillModel> filteredList; // Danh sách sau khi lọc để hiển thị
    private SwipeRefreshLayout swipeRefreshBill;
    private BillAdapter adapter;
    private EditText edtSearchBill;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bill, container, false);

        // 1. Ánh xạ View
        rvBills = view.findViewById(R.id.rvBills);
        swipeRefreshBill = view.findViewById(R.id.swipeRefreshBill);
        edtSearchBill = view.findViewById(R.id.edtSearchBill);

        // 2. Thiết lập RecyclerView
        rvBills.setLayoutManager(new LinearLayoutManager(getContext()));
        billList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Sử dụng filteredList để adapter luôn cập nhật đúng theo ô tìm kiếm
        adapter = new BillAdapter(filteredList);
        rvBills.setAdapter(adapter);

        // 3. Xử lý sự kiện tìm kiếm Real-time
        setupSearchLogic();

        swipeRefreshBill.setOnRefreshListener(this::loadProcessedBills);

        loadProcessedBills();
        return view;
    }

    private void setupSearchLogic() {
        edtSearchBill.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();
        for (BillModel item : billList) {
            // Lọc theo Mã hóa đơn (HDxxx) hoặc Mã bàn (BAxxx)
            if (item.getMaHD().toLowerCase().contains(text.toLowerCase()) ||
                    item.getMaBan().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadProcessedBills() {
        if (swipeRefreshBill != null) swipeRefreshBill.setRefreshing(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<BillModel> tempData = new ArrayList<>();
            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    // CÂU LỆNH SQL: Lấy hóa đơn đã thanh toán
                    String sql = "SELECT d.MaHD, d.MaBan, d.ThoiGianDong, " +
                            "SUM(c.SoLuong * c.DonGia) AS TongTien " +
                            "FROM HoaDon d " +
                            "JOIN ChiTietHoaDon c ON d.MaHD = c.MaHD " +
                            "WHERE d.ThoiGianDong IS NOT NULL " +
                            "GROUP BY d.MaHD, d.MaBan, d.ThoiGianDong " +
                            "ORDER BY d.ThoiGianDong DESC";

                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    while (rs.next()) {
                        tempData.add(new BillModel(
                                rs.getString("MaHD"),
                                rs.getString("MaBan"),
                                rs.getString("ThoiGianDong"),
                                rs.getDouble("TongTien")
                        ));
                    }
                    rs.close(); stmt.close(); conn.close();
                }
            } catch (Exception e) {
                Log.e("SQL_BILL", "Lỗi tải hóa đơn: " + e.getMessage());
            }

            handler.post(() -> {
                // Cập nhật danh sách gốc
                billList.clear();
                billList.addAll(tempData);

                // Cập nhật danh sách hiển thị dựa theo nội dung tìm kiếm hiện tại
                filter(edtSearchBill.getText().toString());

                if (swipeRefreshBill != null) swipeRefreshBill.setRefreshing(false);
            });
        });
    }
}