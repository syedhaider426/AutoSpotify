package com.spring.autospotify;

import java.sql.*;
import java.util.ArrayList;

public class JDBCUtil {
    private static String url;
    private static String driver;

    // Static member holds only one instance of JDBC Connection
    private static JDBCUtil jdbc;

    // JDBCUtil prevents instantiation from any other class
    private JDBCUtil() {
        this.url = "jdbc:postgresql://localhost:5433/postgres";
        this.driver = "org.postgresql.Driver";
    }

    public static JDBCUtil getInstance() {
        if (jdbc == null)
            jdbc = new JDBCUtil();
        return jdbc;
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, "postgres", "9");
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
        return conn;
    }


    // Create Artist Table
    public void createArtistTable() throws SQLException {
        Connection db = getConnection();
        String sql1 = "CREATE TABLE IF NOT EXISTS ARTIST" +
                "(" +
                "Artist TEXT," +
                "SpotifyID TEXT," +
                "PRIMARY KEY (Artist,SpotifyID)" +
                ")";
        PreparedStatement ps1 = db.prepareStatement(sql1);
        ps1.executeUpdate();
        System.out.println("Created Artist Table");
        db.close();
    }

    // Insert Artist into Artist table
    public void insertArtist(String artist, String spotifyID) throws SQLException {
        Connection db = getConnection();
        String sql = "INSERT INTO ARTIST (Artist,SpotifyID) VALUES (?,?) ON CONFLICT ON CONSTRAINT artist_pkey " +
                "DO NOTHING";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, artist);
        ps.setString(2, spotifyID);
        ps.executeUpdate();
        db.close();
    }

    // Get spotify id of an artist
    public String getSpotifyID(String artist) throws SQLException {
        Connection db = getConnection();
        String sql = "SELECT SpotifyID FROM ARTIST Where UPPER(ARTIST) = ?";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, artist);
        ResultSet result = ps.executeQuery();
        String spotifyId = "";
        while (result.next()) {
            spotifyId = result.getString("SpotifyID");
        }
        db.close();
        return spotifyId;
    }

    // Creates Playlist_Tweet table
    public void createPlaylistTweetTable() throws SQLException {
        Connection db = getConnection();
        String sql = "CREATE TABLE IF NOT EXISTS PLAYLIST_TWEET (" +
                "TweetId BIGINT," +
                "PlaylistId TEXT NOT NULL)";
        PreparedStatement ps1 = db.prepareStatement(sql);
        ps1.executeUpdate();
        System.out.println("Created Playlist_Tweet Table");
        db.close();
    }

    // Insert track/album uri and tweet into tweet_track
    public void insertPlaylist_Tweet(Long tweet,String playlistId) throws SQLException {
        Connection db = getConnection();
        String sql = "INSERT INTO PLAYLIST_TWEET (TweetId,PlaylistId) VALUES (?,?)";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setLong(1, tweet);
        ps.setString(2, playlistId);
        ps.executeUpdate();
        System.out.println("Successfully added playlist to db");
        db.close();
    }



    // If the tweet exists, get track associated with it
    public String getPlaylistId(Long tweetId) throws SQLException {
        Connection db = getConnection();
        String sql = "SELECT PlaylistId FROM PLAYLIST_TWEET WHERE TweetId = ?";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setLong(1, tweetId);
        ResultSet result = ps.executeQuery();
        db.close();
        if (result.next()) {
            return result.getString(1);
        }
        return "";
    }

    // Creates Since_Id table
    public void createSinceIdTable() throws SQLException {
        Connection db = getConnection();
        String sql = "CREATE TABLE IF NOT EXISTS SINCE_ID (" +
                "SinceId BIGINT NOT NULL, " +
                "PRIMARY KEY (SinceId) " +
                ")";
        PreparedStatement ps1 = db.prepareStatement(sql);
        ps1.executeUpdate();
        System.out.println("Created Since_Id Table");
        db.close();
    }

    public void insertSinceId(long since_id) {
        try {
            Connection db = getConnection();
            String sql = "INSERT INTO SINCE_ID (SinceId) VALUES (?) ON CONFLICT ON CONSTRAINT since_id_pkey " +
                    "DO UPDATE SET since_id = ?";
            PreparedStatement ps = db.prepareStatement(sql);
            ps.setLong(1,since_id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }


}
