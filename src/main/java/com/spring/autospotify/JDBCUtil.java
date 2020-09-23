package com.spring.autospotify;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/**
 * PostgreSQL database class used to store artists, tweets associated with a specific playlist
 * and storing the newest id of the getMentionsTimeline endpoint (to prevent processing old
 * tweets)
 */
public class JDBCUtil {
    private static String url;
    private static String driver;
    /**
     * Constructor for JDBCUtil
     * -Set the connection url and postgresql driver
     */
    public JDBCUtil() {
        try {
            GetPropertyValues properties = new GetPropertyValues();
            Properties prop = properties.getPropValues();
            url = "jdbc:postgresql://localhost:5432/postgres";
            //this.url = "jdbc:postgresql://" + prop.getProperty("url");
            driver = "org.postgresql.Driver";
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Opens connection to db
     * @return Connection - connection to the database
     */
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

    /**
     * Creates the Artist Table
     */
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

    /**
     * Insert artist name and their corresponding spotifyid
     * @param artist Name of Artist
     * @param spotifyID ID associated with artist in Spotify
     */
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

    /**
     * Gets the spotifyid of an artist
     * @param artist Name of artist
     * @return spotifyid associated with artist if artist exists
     */
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

    /**
     * Creates the PLAYLIST_TWEET Table
     */
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

    /**
     * Stores the tweet id and playlistid to keep track of in case
     * other users request bot on same tweet
     *
     * @param tweet id of tweet that was processed
     * @param playlistId playlistId that was created via Spotify Api
     */
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

    /**
     * Checks to see if the tweetId passed in exists in playlist_tweet table. If
     * it does, return it.
     *
     * @param tweetId id of the tweet that may or may not exist in playlist_tweet
     * @return playlistid if tweet exists, return the playlistid
     */
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

    /**
     * Creates the since_id table
     */
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

    /**
     * Insert the id of the newest tweet from getMentionsTimeline endpoint into table. This only occurs once.
     * @param since_id id of the newest tweet from getMentionsTimeline endpoint
     */
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

    /**
     * Update the since_id
     * @param since_id id of the newest tweet from getMentionsTimeline endpoint
     */
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

    /**
     * Get the since_id
     * @return since_id which is used to get the tweets greater than the since_id
     * (since_id is updated to the highest id from the getMentionsTimeline endpoint)
     */
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
}
