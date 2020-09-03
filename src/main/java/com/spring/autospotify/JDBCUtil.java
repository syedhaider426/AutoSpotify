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

    public void insertArtist(String artist, String spotifyID) throws SQLException {
        String sql = "INSERT INTO ARTIST (Artist,SpotifyID) VALUES (?,?)";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, artist);
        ps.setString(2, spotifyID);
        ps.executeUpdate();
    }

    public String getSpotifyID(String parsedArtist) throws SQLException {
        String sql = "SELECT SpotifyID FROM ARTIST Where UPPER(ARTIST) = ?";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, parsedArtist);
        ResultSet result = ps.executeQuery();
        String spotifyId = "";
        while (result.next()) {
            spotifyId = result.getString("SpotifyID");
        }
        return spotifyId;
    }


}
