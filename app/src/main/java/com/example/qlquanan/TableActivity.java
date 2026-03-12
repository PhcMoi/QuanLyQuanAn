package com.example.qlquanan;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TableActivity extends AppCompatActivity {

    private TextView tvTitle;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        // 1. Ánh xạ View từ layout activity_table.xml
        tvTitle = findViewById(R.id.tvTitle);
        bottomNav = findViewById(R.id.bottom_navigation);

        // 2. Load Fragment mặc định (Sơ đồ bàn) khi mới vào app
        if (savedInstanceState == null) {
            replaceFragment(new TableFragment(), "Sơ đồ bàn");
        }

        // 3. Xử lý sự kiện bấm Tab dưới Bottom Navigation
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                String title = "";

                int id = item.getItemId();
                if (id == R.id.nav_tables) {
                    selectedFragment = new TableFragment();
                    title = "Sơ đồ bàn";
                } else if (id == R.id.nav_bills) {
                    selectedFragment = new BillFragment();
                    title = "Hóa đơn đã xử lý";
                } else if (id == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                    title = "Hồ sơ cá nhân";
                }

                if (selectedFragment != null) {
                    replaceFragment(selectedFragment, title);
                    return true;
                }
                return false;
            });
        }
    }

    private void replaceFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment) // ID này phải khớp với FrameLayout trong activity_table.xml
                .commit();

        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }
}