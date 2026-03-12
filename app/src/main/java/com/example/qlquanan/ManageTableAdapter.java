package com.example.qlquanan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ManageTableAdapter extends RecyclerView.Adapter<ManageTableAdapter.ViewHolder> {
    private List<TableModel> tableList;
    private OnTableAction listener;

    public interface OnTableAction {
        void onEdit(TableModel table);
        void onDelete(TableModel table);
    }

    public ManageTableAdapter(List<TableModel> tableList, OnTableAction listener) {
        this.tableList = tableList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TableModel table = tableList.get(position);
        holder.tvAdminTableName.setText(table.getTenBan());

        String trangThaiStr = table.getTrangThai() == 1 ? "🔴 Đang có khách" : "🟢 Trống";
        holder.tvAdminTableId.setText("Mã: " + table.getMaBan() + " | " + trangThaiStr);

        holder.btnEditTable.setOnClickListener(v -> listener.onEdit(table));
        holder.btnDeleteTable.setOnClickListener(v -> listener.onDelete(table));
    }

    @Override
    public int getItemCount() { return tableList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAdminTableName, tvAdminTableId;
        ImageButton btnEditTable, btnDeleteTable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAdminTableName = itemView.findViewById(R.id.tvAdminTableName);
            tvAdminTableId = itemView.findViewById(R.id.tvAdminTableId);
            btnEditTable = itemView.findViewById(R.id.btnEditTable);
            btnDeleteTable = itemView.findViewById(R.id.btnDeleteTable);
        }
    }
}