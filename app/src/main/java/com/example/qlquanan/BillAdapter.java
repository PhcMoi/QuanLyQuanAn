package com.example.qlquanan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<BillModel> billList;

    public BillAdapter(List<BillModel> billList) {
        this.billList = billList;
    }

    // Thêm hàm này vào trong class BillAdapter
    public void updateList(List<BillModel> newList) {
        this.billList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        BillModel bill = billList.get(position);
        holder.txtBillTableName.setText("Bàn: " + bill.getMaBan());
        holder.txtBillIdTime.setText(bill.getMaHD() + " • " + bill.getThoiGianDong());
        holder.txtBillTotal.setText(String.format("%,.0f đ", bill.getTongTien()));
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView txtBillTableName, txtBillIdTime, txtBillTotal;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBillTableName = itemView.findViewById(R.id.txtBillTableName);
            txtBillIdTime = itemView.findViewById(R.id.txtBillIdTime);
            txtBillTotal = itemView.findViewById(R.id.txtBillTotal);
        }
    }
}