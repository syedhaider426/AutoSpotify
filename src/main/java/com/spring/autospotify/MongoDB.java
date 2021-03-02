package com.spring.autospotify;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.MongoClientSettings;
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
        database = mongoClient.getDatabase("playlistify");
    }


    public void createCollection(String collection) {
        database.createCollection(collection);
        MongoCollection<Document> col1 = database.getCollection(collection);
        System.out.println(col1);
    }

    @Override
    public void createArtistTable() {
        createCollection("artist");
    }

    @Override
    public void createPlaylistTweetTable() {
        createCollection("playlist_tweet");
    }

    @Override
    public void createFutureTweetTable() {
        createCollection("future_tweet");
    }

    @Override
    public void createSinceIdTable() {
        createCollection("since_id");
    }


    @Override
    public String getSpotifyID(String artist) {
        MongoCollection<Document> col1 = database.getCollection("artist");
        Document doc = col1.find(new Document("artist",artist.toUpperCase())).first();
        if(doc == null)
            return "";
        return doc.getString("spotifyID");
    }


    @Override
    public String getPlaylistId(Long tweetId) {
        MongoCollection<Document> col1 = database.getCollection("playlist_tweet");
        Document doc = col1.find(new Document("tweetId",tweetId)).first();
        if(doc == null)
            return "";
        return doc.getString("playlistId");
    }


    @Override
    public void insertArtist(String artist, String spotifyID) {
        // Need to add validation for existing artist
        MongoCollection<Document> col1 = database.getCollection("artist");
        Document doc = col1.find(new Document("artist",artist)).first();
        if(doc != null)
            return;
        doc = new Document("artist", artist.toUpperCase())
                .append("spotifyID", spotifyID);
        col1.insertOne(doc);
    }

    @Override
    public void insertPlaylist_Tweet(Long tweet, String playlistId) {
        // Need to add validation for existing tweet
        MongoCollection<Document> col1 = database.getCollection("playlist_tweet");
        Document document = new Document("tweetId", tweet)
                .append("playlistId", playlistId);
        col1.insertOne(document);
    }

    @Override
    public void insertSinceId(long since_id) {
        MongoCollection<Document> col1 = database.getCollection("since_id");
        Document document = new Document("since_id", since_id);
        col1.insertOne(document);
    }

    @Override
    public void updateSinceId(long since_id) {
        MongoCollection<Document> col1 = database.getCollection("since_id");
        col1.updateMany(new Document(),new Document("$set",new Document("since_id",since_id)));
    }

    @Override
    public long getSinceId() {
        MongoCollection<Document> col1 = database.getCollection("since_id");
        return col1.find().first().getLong("since_id");
    }

    @Override
    public void insertFutureTweet(long tweetId, long inReplyToStatusId) {
        MongoCollection<Document> col1 = database.getCollection("future_tweet");
        Document document = new Document("tweet_id", tweetId)
                .append("inReplyToStatusId", inReplyToStatusId);
        col1.insertOne(document);
    }

    @Override
    public void deleteFutureTweets() {
        MongoCollection<Document> col1 = database.getCollection("future_tweet");
        col1.deleteMany(new Document());
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

