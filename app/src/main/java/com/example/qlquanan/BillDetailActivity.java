package com.example.qlquanan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BillDetailActivity extends AppCompatActivity {

    private double finalTotalAmount = 0.0;
    private ArrayList<OrderItem> orderList;
    private String maHD = ""; // Thêm biến lưu mã Hóa Đơn
    private String maBan = ""; // Thêm biến lưu mã Bàn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        // 1. NHẬN DỮ LIỆU TỪ ORDER ACTIVITY
        orderList = OrderActivity.orderList;
        finalTotalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0.0);

        // Cố gắng lấy mã bàn theo cả 2 key để tránh lỗi (đề phòng OrderActivity gửi TABLE_ID)
        maBan = getIntent().getStringExtra("MA_BAN");
        if (maBan == null) maBan = getIntent().getStringExtra("TABLE_ID");

        // NHẬN MÃ HÓA ĐƠN
        maHD = getIntent().getStringExtra("MA_HD");

        // 2. ÁNH XẠ UI
        findViewById(R.id.btnBackBill).setOnClickListener(v -> finish());
        TextView tvSubTotalBill = findViewById(R.id.tvSubTotalBill);
        TextView tvTotalBill = findViewById(R.id.tvTotalBill);

        // Hiển thị tiền tệ có dấu phẩy
        String formattedTotal = String.format("%,.0f đ", finalTotalAmount);
        tvSubTotalBill.setText(formattedTotal);
        tvTotalBill.setText(formattedTotal);

        // 3. ĐỔ DỮ LIỆU LÊN RECYCLERVIEW
        RecyclerView rvBillItems = findViewById(R.id.rvBillItems);
        rvBillItems.setLayoutManager(new LinearLayoutManager(this));
        if (orderList != null && !orderList.isEmpty()) {
            OrderSummaryAdapter adapter = new OrderSummaryAdapter(orderList, null, true);
            rvBillItems.setAdapter(adapter);
        }

        // 4. XỬ LÝ NÚT IN HÓA ĐƠN
        Button btnPrintBill = findViewById(R.id.btnPrintBill);
        btnPrintBill.setOnClickListener(v -> generateInvoiceHTML());

        // 5. XỬ LÝ NÚT THANH TOÁN (ĐÃ SỬA LỖI 0 VNĐ VÀ THÊM ĐỦ KEY)
        Button btnProceedPayment = findViewById(R.id.btnProceedPayment);
        btnProceedPayment.setOnClickListener(v -> {
            if (maHD == null || maHD.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy Mã hóa đơn!", Toast.LENGTH_SHORT).show();
                return; // Chặn không cho sang trang thanh toán nếu thiếu mã HD
            }

            Intent intent = new Intent(BillDetailActivity.this, PaymentActivity.class);
            // Gửi CHUỖI đã format để bên kia hiện thẳng chữ lên
            intent.putExtra("TOTAL_AMOUNT", formattedTotal);
            // Gửi đúng Key mà PaymentActivity đang chờ
            intent.putExtra("MA_BAN", maBan);
            intent.putExtra("MA_HD", maHD);
            startActivity(intent);
        });
    }

    // Tạo nội dung hóa đơn bằng HTML để in đẹp hơn
    private void generateInvoiceHTML() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2 style='text-align:center;'>HÓA ĐƠN THANH TOÁN</h2>");
        html.append("<hr><table style='width:100%;'>");
        for (OrderItem item : orderList) {
            html.append("<tr><td>").append(item.getFoodItemActivity().getTenMon()).append(" x")
                    .append(item.getQuantity()).append("</td>")
                    .append("<td style='text-align:right;'>")
                    .append(String.format("%,d", item.getFoodItemActivity().getGia() * item.getQuantity()))
                    .append(" đ</td></tr>");
        }
        html.append("</table><hr>");
        html.append("<h3 style='text-align:right;'>Tổng cộng: ")
                .append(String.format("%,.0f", finalTotalAmount)).append(" đ</h3>");
        html.append("</body></html>");

        doPrint(html.toString());
    }

    private void doPrint(String htmlContent) {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("Invoice");
                printManager.print("BillPrintJob", printAdapter, new PrintAttributes.Builder().build());
            }
        });
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null);
    }
}