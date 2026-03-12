package com.example.qlquanan;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.ViewHolder> {

    private Context context;
    private List<PromoModel> list;

    public PromoAdapter(Context context, List<PromoModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PromoModel promo = list.get(position);
        holder.tvCode.setText(promo.getMaKM());
        holder.tvName.setText(promo.getTenKM());
        holder.tvPercent.setText("-" + promo.getPhanTramGiam() + "%");
        holder.tvDate.setText("HSD: " + promo.getNgayBatDau() + " đến " + promo.getNgayKetThuc());

        // Bấm nút này sẽ nhảy sang màn hình Gán món (AssignPromoActivity)
        holder.btnAssign.setOnClickListener(v -> {
            Intent intent = new Intent(context, AssignPromoActivity.class);
            intent.putExtra("MA_KM", promo.getMaKM()); // Gửi mã KM sang bên kia
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvPercent, tvDate;
        Button btnAssign;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvPromoCodeItem);
            tvName = itemView.findViewById(R.id.tvPromoNameItem);
            tvPercent = itemView.findViewById(R.id.tvPromoPercentItem);
            tvDate = itemView.findViewById(R.id.tvPromoDateItem);
            btnAssign = itemView.findViewById(R.id.btnAssignPromoItem);
        }
    }
}