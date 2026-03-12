package com.example.qlquanan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private List<FoodModel> foodList;
    private OnFoodItemClickListener clickListener;
    private OnFoodItemLongClickListener longClickListener;

    public interface OnFoodItemClickListener {
        void onAddClick(FoodModel food);
    }

    public interface OnFoodItemLongClickListener {
        void onLongClick(FoodModel food);
    }

    public FoodAdapter(List<FoodModel> foodList, OnFoodItemClickListener clickListener, OnFoodItemLongClickListener longClickListener) {
        this.foodList = foodList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodModel food = foodList.get(position);
        holder.tvFoodName.setText(food.getTenMon());

        // ==========================================
        // ĐIỂM SÁNG: HIỂN THỊ GIÁ GẠCH NGANG BẰNG CODE
        // ==========================================
        if (food.getPhanTramGiam() > 0) {
            String originalPrice = String.format("%,d đ", food.getGiaGoc());
            String discountPrice = String.format("%,d đ", food.getGia());

            SpannableString spannable = new SpannableString(originalPrice + "\n" + discountPrice);

            // Chữ đầu tiên: Gạch ngang + Màu xám
            spannable.setSpan(new StrikethroughSpan(), 0, originalPrice.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(Color.GRAY), 0, originalPrice.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Chữ thứ hai: Bôi đỏ + In đậm
            spannable.setSpan(new ForegroundColorSpan(Color.RED), originalPrice.length() + 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), originalPrice.length() + 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            holder.tvFoodPrice.setText(spannable);
        } else {
            // Không có khuyến mãi -> Hiện giá bình thường màu đen
            holder.tvFoodPrice.setText(String.format("%,d đ", food.getGia()));
            holder.tvFoodPrice.setTextColor(Color.parseColor("#333333"));
        }

        // Xử lý Hình ảnh
        byte[] hinhAnhBytes = food.getHinhAnh();
        if (hinhAnhBytes != null && hinhAnhBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(hinhAnhBytes, 0, hinhAnhBytes.length);
            holder.imgFood.setImageBitmap(bitmap);
        } else {
            holder.imgFood.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Xử lý phân quyền Click
        if (clickListener != null) {
            holder.btnAdd.setVisibility(View.VISIBLE);
            holder.btnAdd.setOnClickListener(v -> clickListener.onAddClick(food));
        } else {
            holder.btnAdd.setVisibility(View.GONE);
        }

        // Nhấn giữ dành cho Admin
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(food);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvFoodPrice;
        ImageButton btnAdd;
        ImageView imgFood;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            imgFood = itemView.findViewById(R.id.imgFood);
        }
    }
}