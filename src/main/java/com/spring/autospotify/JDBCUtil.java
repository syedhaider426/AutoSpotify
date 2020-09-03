package com.spring.autospotify;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCUtil {
    private final String url;
    private final String driver;
    public JDBCUtil(){
        this.url = "jdbc:postgresql://localhost:5433/postgres";
        this.driver = "org.postgresql.Driver";
    }
    public void init() throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        Connection db = DriverManager.getConnection(url,"postgres","9");
        System.out.println("Succesfully connected to db");
    }
}
