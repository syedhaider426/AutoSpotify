package com.autospotify.database;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.MongoClientSettings;
import com.autospotify.config.GetPropertyValues;
import org.bson.Document;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class MongoDB implements Database {
    private ConnectionString connString;
    private MongoClientSettings settings;
    /**
     * A MongoClient instance represents a pool of connections to the database; you will only need
     * one instance of class MongoClient even with multiple threads.
     */
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDB() throws IOException {
        GetPropertyValues properties = new GetPropertyValues();
        Properties prop = properties.getPropValues();
        connString = new ConnectionString(
                prop.getProperty("url")
        );
        settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .retryWrites(true)
                .build();
        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(prop.getProperty("database"));
    }


    @Override
    public void initialize() {
        if(database.getCollection("since_id").countDocuments() <= 0) {
            createArtistTable();
            createFutureTweetTable();
            createPlaylistTweetTable();
            createSinceIdTable();
        }
    }

    @Override
    public void createArtistTable() {
        database.createCollection("artist");
    }

    @Override
    public void createPlaylistTweetTable() {
        database.createCollection("playlist_tweet");
    }

    @Override
    public void createFutureTweetTable() {
        database.createCollection("future_tweet");
    }

    @Override
    public void createSinceIdTable() {
        database.createCollection("since_id");
    }


    @Override
    public void insertArtist(String artist, String spotifyID) {
        MongoCollection<Document> artistCol = database.getCollection("artist");
        Document doc = artistCol.find(new Document("artist",artist)).first();
        if(doc != null)
            return;
        doc = new Document("artist", artist.toUpperCase())
                .append("spotifyID", spotifyID);
        artistCol.insertOne(doc);
    }

    @Override
    public void insertPlaylist_Tweet(Long tweet, String playlistId) {
        MongoCollection<Document> playlistTweetCol = database.getCollection("playlist_tweet");
        Document document = new Document("tweetId", tweet)
                .append("playlistId", playlistId);
        playlistTweetCol.insertOne(document);
    }

    @Override
    public void insertSinceId(long since_id) {
        MongoCollection<Document> sinceIdCol = database.getCollection("since_id");
        Document document = new Document("since_id", since_id);
        sinceIdCol.insertOne(document);
    }



    @Override
    public String getSpotifyID(String artist) {
        MongoCollection<Document> artistCol = database.getCollection("artist");
        Document doc = artistCol.find(new Document("artist",artist.toUpperCase())).first();
        if(doc == null)
            return "";
        return doc.getString("spotifyID");
    }


    @Override
    public String getPlaylistId(Long tweetId) {
        MongoCollection<Document> playlistTweetCol = database.getCollection("playlist_tweet");
        Document doc = playlistTweetCol.find(new Document("tweetId",tweetId)).first();
        if(doc == null)
            return "";
        return doc.getString("playlistId");
    }


    @Override
    public long getSinceId() {
        MongoCollection<Document> sinceIdCol = database.getCollection("since_id");
        Document doc = sinceIdCol.find(new Document()).first();
        if(doc == null)
            return -1L;
        return doc.getLong("since_id");
    }

    @Override
    public void updateSinceId(long since_id) {
        MongoCollection<Document> sinceIdCol = database.getCollection("since_id");
        sinceIdCol.updateMany(new Document(),new Document("$set",new Document("since_id",since_id)));
    }


    @Override
    public void insertFutureTweet(long tweetId, long inReplyToStatusId) {
        MongoCollection<Document> futureTweetCol = database.getCollection("future_tweet");
        Document document = new Document("tweet_id", tweetId)
                .append("inReplyToStatusId", inReplyToStatusId);
        futureTweetCol.insertOne(document);
    }

    @Override
    public void deleteFutureTweets() {
        MongoCollection<Document> futureTweetCol = database.getCollection("future_tweet");
        futureTweetCol.deleteMany(new Document());
    }

    @Override
    public Map<Long, Long> getFutureTweets() {
        Map<Long, Long> tweets = new LinkedHashMap<>(); //process in order
        MongoCollection<Document> col1 = database.getCollection("future_tweet");
        Consumer<Document> printConsumer = document ->
                tweets.put(document.getLong("tweet_id"),document.getLong("inReplyToStatusId"));
        col1.find().forEach(printConsumer);
        return tweets;
    }
}

