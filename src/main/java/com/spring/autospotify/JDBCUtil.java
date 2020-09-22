package com.spring.autospotify;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class JDBCUtil {
    private static String url;
    private static String driver;

    // Static member holds only one instance of JDBC Connection
    private static JDBCUtil jdbc;

    // JDBCUtil prevents instantiation from any other class
    private JDBCUtil() {
        try {
            GetPropertyValues properties = new GetPropertyValues();
            Properties prop = properties.getPropValues();
            this.url = "jdbc:postgresql://localhost:5432/postgres";
            //this.url = "jdbc:postgresql://" + prop.getProperty("url");
            this.driver = "org.postgresql.Driver";
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static JDBCUtil getInstance() {
        if (jdbc == null)
            jdbc = new JDBCUtil();
        return jdbc;
    }

    public static Connection getConnection() {
        Connection conn = null;
        Properties prop;
        try {
            Class.forName(driver);
            GetPropertyValues properties = new GetPropertyValues();
            prop = properties.getPropValues();
            conn = DriverManager.getConnection(url, prop.getProperty("username"), prop.getProperty("password"));
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            ex.printStackTrace();
        }
        return conn;
    }


    // Create Artist Table
    public void createArtistTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ARTIST" +
                "(" +
                "Artist TEXT," +
                "SpotifyID TEXT," +
                "PRIMARY KEY (Artist,SpotifyID)" +
                ")";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql);
        ) {
            ps1.executeUpdate();
            System.out.println("Created Artist Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Insert Artist into Artist table
    public void insertArtist(String artist, String spotifyID) {
        String sql = "INSERT INTO ARTIST (Artist,SpotifyID) VALUES (?,?) ON CONFLICT ON CONSTRAINT artist_pkey " +
                "DO NOTHING";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setString(1, artist);
            ps.setString(2, spotifyID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    // Get spotify id of an artist
    public String getSpotifyID(String artist) {
        String sql = "SELECT SpotifyID FROM ARTIST Where UPPER(ARTIST) = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setString(1, artist);
            try (ResultSet result = ps.executeQuery();) {
                String spotifyId = "";
                while (result.next()) {
                    spotifyId = result.getString("SpotifyID");
                }
                return spotifyId;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Creates Playlist_Tweet table
    public void createPlaylistTweetTable() {
        String sql = "CREATE TABLE IF NOT EXISTS PLAYLIST_TWEET (" +
                "TweetId BIGINT," +
                "PlaylistId TEXT NOT NULL)";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql);
        ) {
            ps1.executeUpdate();
            System.out.println("Created Playlist_Tweet Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Insert track/album uri and tweet into tweet_track
    public void insertPlaylist_Tweet(Long tweet, String playlistId) {
        String sql = "INSERT INTO PLAYLIST_TWEET (TweetId,PlaylistId) VALUES (?,?)";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setLong(1, tweet);
            ps.setString(2, playlistId);
            ps.executeUpdate();
            System.out.println("Successfully added playlist to db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    // If the tweet exists, get track associated with it
    public String getPlaylistId(Long tweetId) {
        String sql = "SELECT PlaylistId FROM PLAYLIST_TWEET WHERE TweetId = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setLong(1, tweetId);
            try (ResultSet result = ps.executeQuery();) {
                if (result.next()) {
                    return result.getString(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    // Creates Since_Id table
    public void createSinceIdTable() {
        String sql = "CREATE TABLE IF NOT EXISTS SINCE_ID (" +
                "since_id BIGINT NOT NULL, " +
                "PRIMARY KEY (since_id) " +
                ")";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql);
        ) {
            ps1.executeUpdate();
            System.out.println("Created Since_Id Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void insertSinceId(long since_id) {
        String sql = "INSERT INTO SINCE_ID (since_id) VALUES (?) ON CONFLICT ON CONSTRAINT since_id_pkey " +
                "DO UPDATE SET since_id = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setLong(1, since_id);
            ps.setLong(2, since_id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateSinceId(long since_id) {
        String sql = "UPDATE SINCE_ID SET since_id = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            ps.setLong(1, since_id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    public long getSinceId() {
        String sql = "SELECT since_id FROM SINCE_ID";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql);
        ) {
            //ps.setLong(1, since_id);
            try (ResultSet resultSet = ps.executeQuery();) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        ;
        return 1L;
    }
}
