package com.example.qlquanan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private List<TableModel> tableList;
    private OnTableClickListener clickListener;
    private OnTableLongClickListener longClickListener;

    // BIẾN CỜ PHÂN QUYỀN
    private boolean isAdminMode = false;

    public interface OnTableClickListener {
        void onTableClick(TableModel table);
    }

    public interface OnTableLongClickListener {
        void onTableLongClick(TableModel table);
    }

    // Constructor giữ nguyên để không làm lỗi code cũ
    public TableAdapter(List<TableModel> tableList, OnTableClickListener clickListener, OnTableLongClickListener longClickListener) {
        this.tableList = tableList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    // Hàm để AdminActivity gọi khi muốn "biến hình" giao diện
    public void setAdminMode(boolean adminMode) {
        this.isAdminMode = adminMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TableModel table = tableList.get(position);
        holder.tvTableName.setText(table.getTenBan());

        if (isAdminMode) {
            // --- GIAO DIỆN DÀNH RIÊNG CHO ADMIN ---
            holder.viewTopIndicator.setBackgroundColor(Color.parseColor("#9E9E9E")); // Màu xám trung tính
            holder.tvTableStatus.setText("ID: " + table.getMaBan()); // Hiện mã bàn để dễ quản lý
            holder.tvTableStatus.setTextColor(Color.GRAY);

            // Ở chế độ Admin, vô hiệu hóa Long Click để tránh gộp nhầm
            holder.itemView.setOnLongClickListener(null);
        } else {
            // --- GIAO DIỆN DÀNH CHO NHÂN VIÊN (VẬN HÀNH) ---
            if (table.getTrangThai() == 1) {
                holder.viewTopIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
                holder.tvTableStatus.setText("CÓ KHÁCH");
                holder.tvTableStatus.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.viewTopIndicator.setBackgroundColor(Color.parseColor("#00E676"));
                holder.tvTableStatus.setText("TRỐNG");
                holder.tvTableStatus.setTextColor(Color.parseColor("#00E676"));
            }

            // Nhấn giữ để Chuyển/Gộp bàn
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onTableLongClick(table);
                return true;
            });
        }

        // Sự kiện Click chung (Xử lý logic khác nhau trong Activity)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTableClick(table);
        });
    }

    @Override
    public int getItemCount() {
        return tableList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewTopIndicator;
        TextView tvTableName, tvTableStatus, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewTopIndicator = itemView.findViewById(R.id.viewTopIndicator);
            tvTableName = itemView.findViewById(R.id.tvTableName);
            tvTableStatus = itemView.findViewById(R.id.tvTableStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}