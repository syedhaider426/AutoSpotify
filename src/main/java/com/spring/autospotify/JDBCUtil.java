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
    }

    // Get spotify id of an artist
    public String getSpotifyID(String parsedArtist) throws SQLException {
        Connection db = getConnection();
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

    // Creates Tweet_URI table
    public void createUriTweetTable() throws SQLException {
        Connection db = getConnection();
        String sql = "CREATE TABLE IF NOT EXISTS TWEET_URI (" +
                "TweetId BIGINT," +
                "SpotifyURI TEXT NOT NULL)";
        PreparedStatement ps1 = db.prepareStatement(sql);
        ps1.executeUpdate();
        System.out.println("Created Tweet_Uri Table");
    }

    // Insert track/album uri and tweet into tweet_track
    public void insertUriTweet(ArrayList<String> spotifyUriList, Long tweet) throws SQLException {
        Connection db = getConnection();
        String sql = "INSERT INTO TWEET_URI (TweetId,SpotifyURI) VALUES (?,?)";
        PreparedStatement ps = db.prepareStatement(sql);
        for (int x = 0; x < spotifyUriList.size(); x++) {
            ps.setLong(1, tweet);
            ps.setString(2, spotifyUriList.get(x));
            ps.addBatch();
        }
        ps.executeBatch();
        System.out.println("Successfully added songs to db");
    }

    // If the tweet exists, get track associated with it
    public ArrayList<String> getTracks(Long tweetId) throws SQLException {
        Connection db = getConnection();
        ArrayList<String> tracks = new ArrayList<>();
        String sql = "SELECT SpotifyURI FROM TWEET_URI WHERE TweetId = ?";
        PreparedStatement ps = db.prepareStatement(sql);
        ps.setLong(1, tweetId);
        ResultSet result = ps.executeQuery();
        while (result.next()) {
            tracks.add(result.getString(1));
        }
        return tracks;
    }

    public void insertSinceId(long since_id) {
        String sql = "INSERT";
    }


}
