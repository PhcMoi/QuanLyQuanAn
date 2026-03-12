package com.example.qlquanan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AssignPromoAdapter extends RecyclerView.Adapter<AssignPromoAdapter.ViewHolder> {

    private List<FoodAssignModel> list;
    private String currentMaKM; // Mã KM mà Admin đang chọn để gán
    private OnItemCheckListener checkListener;

    public interface OnItemCheckListener {
        void onCheckChanged(int totalSelected);
    }

    public AssignPromoAdapter(List<FoodAssignModel> list, String currentMaKM, OnItemCheckListener listener) {
        this.list = list;
        this.currentMaKM = currentMaKM;
        this.checkListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodAssignModel food = list.get(position);
        holder.tvFoodName.setText(food.getTenMon());
        holder.tvFoodPrice.setText(String.format("%,.0f đ", food.getGiaGoc()));

        // Gỡ bỏ Listener cũ để tránh lỗi Recycle
        holder.cbSelectFood.setOnCheckedChangeListener(null);
        holder.cbSelectFood.setChecked(food.isSelected());

        // Hiện cảnh báo nếu món này ĐANG CÓ mã khác (Không phải null, và không phải mã hiện tại)
        if (food.getMaKMDangCo() != null && !food.getMaKMDangCo().isEmpty() && !food.getMaKMDangCo().equalsIgnoreCase(currentMaKM)) {
            holder.tvCurrentPromoTag.setVisibility(View.VISIBLE);
            holder.tvCurrentPromoTag.setText("Đang có mã: " + food.getMaKMDangCo());
        } else {
            holder.tvCurrentPromoTag.setVisibility(View.GONE);
        }

        // Bắt sự kiện Check/Uncheck an toàn
        holder.cbSelectFood.setOnCheckedChangeListener((buttonView, isChecked) -> {
            food.setSelected(isChecked);

            // Đếm số lượng món đang được chọn để gửi về Activity
            int count = 0;
            for (FoodAssignModel f : list) {
                if (f.isSelected()) count++;
            }
            if (checkListener != null) checkListener.onCheckChanged(count);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectFood;
        TextView tvFoodName, tvFoodPrice, tvCurrentPromoTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectFood = itemView.findViewById(R.id.cbSelectFood);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvCurrentPromoTag = itemView.findViewById(R.id.tvCurrentPromoTag);
        }
    }
}