package com.example.qlquanan;

import android.os.StrictMode;
import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnection {
    // Thay đổi thông tin theo Server của bạn
    private static String ip = "192.168.56.1"; // IP máy tính chạy SQL Server
    private static String port = "1433";
    private static String database = "QLQuanAn";
    private static String username = "sa";
    private static String password = "123";

    public Connection connection() {
        Connection conn = null;
        String ConnString = null;
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            ConnString = "jdbc:jtds:sqlserver://" + ip + ":" + port + "/" + database;
            conn = DriverManager.getConnection(ConnString, username, password);
        } catch (SQLException se) {
            Log.e("ERROR", se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("ERROR", e.getMessage());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }
        return conn;
    }
}
