package com.spring.autospotify;

import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * PostgreSQL database class used to store artists, tweets associated with a specific playlist
 * and storing the newest id of the getMentionsTimeline endpoint (to prevent processing old
 * tweets)
 */
public class PostgresDB implements Database {
    private static String url;
    private static String driver;

    public PostgresDB() {
        try {
            GetPropertyValues properties = new GetPropertyValues();
            Properties prop = properties.getPropValues();
            url = "jdbc:postgresql://" + prop.getProperty("url");
            driver = "org.postgresql.Driver";
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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

    public void createArtistTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ARTIST" +
                "(" +
                "Artist TEXT," +
                "SpotifyID TEXT," +
                "PRIMARY KEY (Artist,SpotifyID)" +
                ")";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql)
        ) {
            ps1.executeUpdate();
            System.out.println("Created Artist Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void insertArtist(String artist, String spotifyID) {
        String sql = "INSERT INTO ARTIST (Artist,SpotifyID) VALUES (?,?) ON CONFLICT ON CONSTRAINT artist_pkey " +
                "DO NOTHING";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setString(1, artist);
            ps.setString(2, spotifyID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public String getSpotifyID(String artist) {
        String sql = "SELECT SpotifyID FROM ARTIST Where UPPER(ARTIST) = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setString(1, artist);
            try (ResultSet result = ps.executeQuery()) {
                String spotifyId = "";
                if (result.next()) {
                    spotifyId = result.getString("SpotifyID");
                }
                return spotifyId;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void createPlaylistTweetTable() {
        String sql = "CREATE TABLE IF NOT EXISTS PLAYLIST_TWEET (" +
                "TweetId BIGINT," +
                "PlaylistId TEXT NOT NULL)";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql)
        ) {
            ps1.executeUpdate();
            System.out.println("Created Playlist_Tweet Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void insertPlaylist_Tweet(Long tweet, String playlistId) {
        String sql = "INSERT INTO PLAYLIST_TWEET (TweetId,PlaylistId) VALUES (?,?)";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setLong(1, tweet);
            ps.setString(2, playlistId);
            ps.executeUpdate();
            System.out.println("Successfully added playlist to db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public String getPlaylistId(Long tweetId) {
        String sql = "SELECT PlaylistId FROM PLAYLIST_TWEET WHERE TweetId = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setLong(1, tweetId);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    return result.getString(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public void createSinceIdTable() {
        String sql = "CREATE TABLE IF NOT EXISTS SINCE_ID (" +
                "since_id BIGINT NOT NULL, " +
                "PRIMARY KEY (since_id) " +
                ")";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql)
        ) {
            ps1.executeUpdate();
            System.out.println("Created Since_Id Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void insertSinceId(long since_id) {
        String sql = "INSERT INTO SINCE_ID (since_id) VALUES (?)";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setLong(1, since_id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateSinceId(long since_id) {
        String sql = "UPDATE SINCE_ID SET since_id = ?";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
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
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 1L;
    }

    public void createFutureTweetTable() {
        String sql = "CREATE TABLE IF NOT EXISTS FUTURE_TWEET (" +
                "tweet_id BIGINT NOT NULL," +
                "inReplyToStatusId BIGINT NOT NULL," +
                "PRIMARY KEY (tweet_id) " +
                ")";
        try (
                Connection db = getConnection();
                PreparedStatement ps1 = db.prepareStatement(sql)
        ) {
            ps1.executeUpdate();
            System.out.println("Created Future Tweet Table");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void insertFutureTweet(long tweetId, long inReplyToStatusId) {
        String sql = "INSERT INTO FUTURE_TWEET (tweet_id, inReplyToStatusId) VALUES (?,?)";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.setLong(1, tweetId);
            ps.setLong(2, inReplyToStatusId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteFutureTweets() {
        String sql = "DELETE FROM FUTURE_TWEET";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Map<Long, Long> getFutureTweets() {
        Map<Long, Long> tweets = new LinkedHashMap<>();
        String sql = "SELECT tweet_id, inReplyToStatusId FROM FUTURE_TWEET";
        try (
                Connection db = getConnection();
                PreparedStatement ps = db.prepareStatement(sql)
        ) {
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    tweets.put(result.getLong(1), result.getLong(2));
                }
                return tweets;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return tweets;
    }


}
