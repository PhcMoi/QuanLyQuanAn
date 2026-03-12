package com.example.qlquanan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.OrderViewHolder> {
    private List<OrderItem> orderList;
    private OnQuantityChangeListener listener;
    private boolean isReadOnly = false; // Cờ đánh dấu chế độ Chỉ Đọc (Mặc định là false)

    public interface OnQuantityChangeListener {
        void onIncrease(OrderItem item);
        void onDecrease(OrderItem item);
    }

    // Constructor 1: Dành cho màn hình Gọi món (Giữ nguyên như cũ để không lỗi OrderActivity)
    public OrderSummaryAdapter(List<OrderItem> orderList, OnQuantityChangeListener listener) {
        this.orderList = orderList;
        this.listener = listener;
        this.isReadOnly = false;
    }

    // Constructor 2: Dành riêng cho màn hình In Bill (Kích hoạt cờ Read-only)
    public OrderSummaryAdapter(List<OrderItem> orderList, OnQuantityChangeListener listener, boolean isReadOnly) {
        this.orderList = orderList;
        this.listener = listener;
        this.isReadOnly = isReadOnly;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_summary, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderItem item = orderList.get(position);

        // Gắn tên món
        holder.tvOrderName.setText(item.getFoodItemActivity().getTenMon());

        // Tính ra tiền tổng rồi định dạng lại theo kiểu Số Nguyên (%,d)
        int tongTienMon = item.getFoodItemActivity().getGia() * item.getQuantity();
        holder.tvOrderPrice.setText(String.format("%,d đ", tongTienMon));

        // Gắn số lượng
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // ==========================================
        // ĐIỂM SÁNG: KIỂM TRA CỜ READ-ONLY ĐỂ ẨN NÚT
        // ==========================================
        if (isReadOnly) {
            holder.btnPlus.setVisibility(View.GONE);
            holder.btnMinus.setVisibility(View.GONE);
        } else {
            holder.btnPlus.setVisibility(View.VISIBLE);
            holder.btnMinus.setVisibility(View.VISIBLE);

            // Chỉ bắt sự kiện nếu listener không bị null
            holder.btnPlus.setOnClickListener(v -> {
                if (listener != null) listener.onIncrease(item);
            });
            holder.btnMinus.setOnClickListener(v -> {
                if (listener != null) listener.onDecrease(item);
            });
        }
    }

    @Override
    public int getItemCount() { return orderList.size(); }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderName, tvOrderPrice, tvQuantity;
        ImageButton btnPlus, btnMinus;
        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderName = itemView.findViewById(R.id.tvOrderName);
            tvOrderPrice = itemView.findViewById(R.id.tvOrderPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}