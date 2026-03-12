package com.example.qlquanan;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    private Context context;
    private List<StaffModel> staffList;
    private Runnable onDataChanged;
    private OnEditClickListener editListener; // Ống nối truyền sự kiện Sửa

    // Interface để Activity bắt được sự kiện Sửa
    public interface OnEditClickListener {
        void onEdit(StaffModel staff);
    }

    public StaffAdapter(Context context, List<StaffModel> staffList, Runnable onDataChanged, OnEditClickListener editListener) {
        this.context = context;
        this.staffList = staffList;
        this.onDataChanged = onDataChanged;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        StaffModel staff = staffList.get(position);

        holder.tvStaffName.setText(staff.getTenNV());
        holder.tvStaffRole.setText("Vai trò: " + (staff.getVaiTro().equals("QuanLy") ? "Quản lý" : "Nhân viên"));
        holder.tvStaffPhone.setText("SĐT: " + staff.getSdt());
        holder.tvStaffUsername.setText("User: " + staff.getUsername());

        // CHỨC NĂNG XÓA
        holder.btnDeleteStaff.setOnClickListener(v -> {
            if (staff.getUsername().equalsIgnoreCase("admin")) {
                Toast.makeText(context, "Cảnh báo: Không thể xóa tài khoản Quản trị gốc!", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận Xóa")
                    .setMessage("Bạn có chắc muốn xóa nhân viên " + staff.getTenNV() + "?")
                    .setPositiveButton("Xóa ngay", (dialog, which) -> deleteStaffInSQL(staff.getMaNV()))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // CHỨC NĂNG SỬA: Truyền object staff qua cho Activity xử lý
        holder.btnEditStaff.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEdit(staff);
            }
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    private void deleteStaffInSQL(String maNV) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isSuccess = false;
            try {
                SQLConnection sqlConnection = new SQLConnection();
                Connection conn = sqlConnection.connection();
                if (conn != null) {
                    String sql = "UPDATE NhanVien SET TrangThai = 0 WHERE MaNV = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, maNV);
                    stmt.executeUpdate();
                    stmt.close();
                    conn.close();
                    isSuccess = true;
                }
            } catch (Exception e) {
                Log.e("SQL_DELETE", "Lỗi: " + e.getMessage());
            }

            final boolean finalSuccess = isSuccess;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(context, "Đã xóa thành công!", Toast.LENGTH_SHORT).show();
                    if (onDataChanged != null) onDataChanged.run();
                } else {
                    Toast.makeText(context, "Lỗi khi xóa dữ liệu!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView tvStaffName, tvStaffRole, tvStaffPhone, tvStaffUsername;
        ImageButton btnEditStaff, btnDeleteStaff;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvStaffRole = itemView.findViewById(R.id.tvStaffRole);
            tvStaffPhone = itemView.findViewById(R.id.tvStaffPhone);
            tvStaffUsername = itemView.findViewById(R.id.tvStaffUsername);
            btnEditStaff = itemView.findViewById(R.id.btnEditStaff);
            btnDeleteStaff = itemView.findViewById(R.id.btnDeleteStaff);
        }
    }
}