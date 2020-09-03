package com.spring.autospotify;

import java.sql.*;

public class JDBCUtil {
    private final String url;
    private final String driver;
    private Connection db;

    public JDBCUtil() throws SQLException {
        this.url = "jdbc:postgresql://localhost:5433/postgres";
        this.driver = "org.postgresql.Driver";
        this.db = null;
    }

    public void init() throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, "postgres", "9");
        this.db = conn;
    }

    public void createArtistTable() throws SQLException {
        String sql1 = "CREATE TABLE IF NOT EXISTS ARTIST" +
                "(" +
                "Artist UNIQUE NOT NULL TEXT," +
                "SpotifyID UNIQUE NOT NULL TEXT" +
                ")";
        PreparedStatement ps1 = db.prepareStatement(sql1);
        ps1.executeUpdate();
    }



    public String selectArtist(String originalArtist) throws SQLException {
        String sql = "SELECT Artist,SpotifyID FROM ARTIST Where Artist = ?";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, originalArtist);
        ResultSet result = ps.executeQuery();
        String spotifyId = "";
        while (result.next()) {
            spotifyId = result.getString("SpotifyID");
        }
        return spotifyId;
    }


}
