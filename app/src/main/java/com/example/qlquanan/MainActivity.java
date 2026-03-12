package com.example.qlquanan;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_table);

        // Xử lý Window Insets an toàn
        if (findViewById(android.R.id.content) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- KIỂM TRA KẾT NỐI TRÊN LUỒNG RIÊNG ---
        checkSQLConnectionAsync();
    }

    private void checkSQLConnectionAsync() {
        // Tạo một luồng chạy ngầm để không gây treo App
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            SQLConnection sqlConnection = new SQLConnection();
            Connection conn = sqlConnection.connection();

            // Sau khi kết nối xong (thành công hoặc thất bại), quay lại luồng chính để hiện Toast
            handler.post(() -> {
                if (conn != null) {
                    Toast.makeText(MainActivity.this, "Kết nối SQL Server THÀNH CÔNG!", Toast.LENGTH_SHORT).show();
                    try {
                        executor.execute(() -> {
                            try { conn.close(); } catch (Exception e) { e.printStackTrace(); }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Kết nối THẤT BẠI! Kiểm tra IP và Firewall.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
