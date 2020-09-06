package com.spring.autospotify;

import java.sql.*;
import java.util.ArrayList;

public class JDBCUtil {
    private final String url;
    private final String driver;
    private Connection db;

    public JDBCUtil() throws SQLException, ClassNotFoundException {
        this.url = "jdbc:postgresql://localhost:5433/postgres";
        this.driver = "org.postgresql.Driver";
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, "postgres", "9");
        System.out.println("Connected to db");
        this.db = conn;
    }


    // Create Artist Table
    public void createArtistTable() throws SQLException {
        String sql1 = "CREATE TABLE IF NOT EXISTS ARTIST" +
                "(" +
                "Artist TEXT," +
                "SpotifyID TEXT," +
                "PRIMARY KEY (Artist,SpotifyID)" +
                ")";
        PreparedStatement ps1 = db.prepareStatement(sql1);
        ps1.executeUpdate();
        System.out.println("Created Artist Table");
    }

    // Insert Artist into Artist table
    public void insertArtist(String artist, String spotifyID) throws SQLException {
        String sql = "INSERT INTO ARTIST (Artist,SpotifyID) VALUES (?,?) ON CONFLICT ON CONSTRAINT artist_pkey " +
                "DO NOTHING";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, artist);
        ps.setString(2, spotifyID);
        ps.executeUpdate();
    }

    // Get spotify id of an artist
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

    public void createUriTweetTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS TWEET_URI (" +
                "TweetId BIGINT PRIMARY KEY," +
                "SpotifyURI TEXT NOT NULL)";
        PreparedStatement ps1 = db.prepareStatement(sql);
        ps1.executeUpdate();
        System.out.println("Created Tweet_Uri Table");
    }

    // Insert track/album uri and tweet into tweet_track
    public void insertUriTweet(ArrayList<String> spotifyUriList, Long tweet) throws SQLException{
        String sql = "INSERT INTO TWEET_URI (TweetId,SpotifyURI) VALUES (?,?)";
        PreparedStatement ps = db.prepareStatement(sql);
        for(int x = 0; x < spotifyUriList.size(); x++) {
            ps.setLong(1,tweet);
            ps.setString(2, spotifyUriList.get(x));
            ps.addBatch();
        }
        ps.executeBatch();
        System.out.println("Successfully added songs to db");
    }




}
