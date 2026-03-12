package com.example.qlquanan;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private Spinner spinnerTimeFilter;
    private TextView tvTotalRevenue;
    private HorizontalBarChart chartTopFood;
    private final String[] timeFilters = {"Hôm nay", "Chọn ngày cụ thể", "Chọn tháng/năm", "Năm nay", "Tất cả"};
    private int selectedYear, selectedMonth, selectedDay;
    private ArrayList<FoodReport> currentReportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        chartTopFood = findViewById(R.id.chartTopFood);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Nút Xuất Excel
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());

        Calendar c = Calendar.getInstance();
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH) + 1;
        selectedDay = c.get(Calendar.DAY_OF_MONTH);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, timeFilters);
        spinnerTimeFilter.setAdapter(adapter);

        setupChartAppearance();

        spinnerTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = timeFilters[position];
                if (selected.equals("Chọn ngày cụ thể")) showDatePicker();
                else if (selected.equals("Chọn tháng/năm")) showMonthYearPicker();
                else loadDashboardData(selected, -1, -1, -1);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChartAppearance() {
        chartTopFood.getDescription().setEnabled(false);
        chartTopFood.getLegend().setEnabled(false);
        chartTopFood.setDrawValueAboveBar(true);
        chartTopFood.setExtraOffsets(80, 10, 50, 10);
        XAxis xAxis = chartTopFood.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        chartTopFood.getAxisLeft().setAxisMinimum(0f);
        chartTopFood.getAxisRight().setEnabled(false);
    }

    // --- HÀM CHỌN NGÀY ---
    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            loadDashboardData("Ngày cụ thể", dayOfMonth, month + 1, year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // --- HÀM CHỌN THÁNG/NĂM ---
    private void showMonthYearPicker() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_month_year_picker);

        NumberPicker monthPicker = dialog.findViewById(R.id.picker_month);
        NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmPicker);

        monthPicker.setMinValue(1); monthPicker.setMaxValue(12);
        monthPicker.setValue(selectedMonth);

        yearPicker.setMinValue(2020); yearPicker.setMaxValue(2030);
        yearPicker.setValue(selectedYear);

        btnConfirm.setOnClickListener(v -> {
            loadDashboardData("Tháng cụ thể", -1, monthPicker.getValue(), yearPicker.getValue());
            dialog.dismiss();
        });
        dialog.show();
    }

    private void loadDashboardData(String filterType, int day, int month, int year) {
        tvTotalRevenue.setText("Đang tính...");
        chartTopFood.clear();
        currentReportList.clear();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            double totalRevenue = 0;
            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> foodNames = new ArrayList<>();
            boolean isSuccess = false;

            try {
                Connection conn = new SQLConnection().connection();
                if (conn != null) {
                    StringBuilder timeCondition = new StringBuilder();
                    if (filterType.equals("Hôm nay")) timeCondition.append("AND d.NgayLap = CAST(GETDATE() AS DATE) ");
                    else if (filterType.equals("Ngày cụ thể")) timeCondition.append("AND DAY(d.NgayLap) = ").append(day).append(" AND MONTH(d.NgayLap) = ").append(month).append(" AND YEAR(d.NgayLap) = ").append(year);
                    else if (filterType.equals("Tháng cụ thể")) timeCondition.append("AND MONTH(d.NgayLap) = ").append(month).append(" AND YEAR(d.NgayLap) = ").append(year);
                    else if (filterType.equals("Năm nay")) timeCondition.append("AND YEAR(d.NgayLap) = YEAR(GETDATE()) ");

                    String sqlRev = "SELECT SUM(TongTien) AS Total FROM HoaDon d WHERE ThoiGianDong IS NOT NULL " + timeCondition;
                    Statement stRev = conn.createStatement();
                    ResultSet rsRev = stRev.executeQuery(sqlRev);
                    if (rsRev.next()) totalRevenue = rsRev.getDouble("Total");

                    String sqlTop = "SELECT TOP 10 m.TenMon, SUM(c.SoLuong) AS TotalQty, SUM(c.ThanhTien) AS TotalRevenue " +
                            "FROM ChiTietHoaDon c JOIN HoaDon d ON c.MaHD = d.MaHD " +
                            "JOIN MonAn m ON c.MaMon = m.MaMon " +
                            "WHERE d.ThoiGianDong IS NOT NULL " + timeCondition +
                            "GROUP BY m.TenMon ORDER BY TotalQty ASC";

                    Statement stTop = conn.createStatement();
                    ResultSet rsTop = stTop.executeQuery(sqlTop);
                    int idx = 0;
                    while (rsTop.next()) {
                        String name = rsTop.getString("TenMon");
                        int qty = rsTop.getInt("TotalQty");
                        double rev = rsTop.getDouble("TotalRevenue");
                        foodNames.add(name);
                        entries.add(new BarEntry(idx++, qty));
                        currentReportList.add(new FoodReport(name, qty, rev));
                    }
                    conn.close();
                    isSuccess = true;
                }
            } catch (Exception e) { Log.e("SQL_DASH", e.getMessage()); }

            final boolean fSuccess = isSuccess;
            final double fRev = totalRevenue;
            handler.post(() -> {
                if (fSuccess) {
                    tvTotalRevenue.setText(String.format("%,.0f đ", fRev));
                    if (!entries.isEmpty()) updateChart(entries, foodNames);
                    else Toast.makeText(this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // --- HÀM CẬP NHẬT BIỂU ĐỒ ---
    private void updateChart(ArrayList<BarEntry> entries, ArrayList<String> foodNames) {
        BarDataSet set = new BarDataSet(entries, "Số lượng");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(12f);
        set.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });

        chartTopFood.setData(new BarData(set));
        chartTopFood.getXAxis().setValueFormatter(new IndexAxisValueFormatter(foodNames));
        chartTopFood.getXAxis().setLabelCount(foodNames.size());
        chartTopFood.animateY(800);
        chartTopFood.invalidate();
    }

    private void exportToExcel() {
        if (currentReportList.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu!", Toast.LENGTH_SHORT).show();
            return;
        }
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Doanh Thu");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Tên Món");
        header.createCell(1).setCellValue("Số Lượng");
        header.createCell(2).setCellValue("Doanh Thu (VNĐ)");

        for (int i = 0; i < currentReportList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(currentReportList.get(i).name);
            row.createCell(1).setCellValue(currentReportList.get(i).qty);
            row.createCell(2).setCellValue(currentReportList.get(i).revenue);
        }

        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Bao_Cao_" + System.currentTimeMillis() + ".xlsx");
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
            workbook.close();
            Toast.makeText(this, "Đã xuất file vào Downloads!", Toast.LENGTH_LONG).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class FoodReport {
        String name; int qty; double revenue;
        FoodReport(String n, int q, double r) { name = n; qty = q; revenue = r; }
    }
}