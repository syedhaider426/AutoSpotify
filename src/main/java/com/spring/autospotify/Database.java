package com.spring.autospotify;

import java.util.Map;

public interface Database {
    void createArtistTable();

    /**
     * Insert artist name and their corresponding spotifyid
     *
     * @param artist    Name of Artist
     * @param spotifyID ID associated with artist in Spotify
     */
    void insertArtist(String artist, String spotifyID);

    /**
     * Gets the spotifyid of an artist
     *
     * @param artist Name of artist
     * @return spotifyid associated with artist if artist exists
     */
    String getSpotifyID(String artist);

    /**
     * Creates the PLAYLIST_TWEET Table
     */
    void createPlaylistTweetTable();

    /**
     * Stores the tweet id and playlistid to keep track of in case
     * other users request bot on same tweet
     *
     * @param tweet      id of tweet that was processed
     * @param playlistId playlistId that was created via Spotify Api
     */
    void insertPlaylist_Tweet(Long tweet, String playlistId);


    /**
     * Checks to see if the tweetId passed in exists in playlist_tweet table. If
     * it does, return it.
     *
     * @param tweetId id of the tweet that may or may not exist in playlist_tweet
     * @return playlistid if tweet exists, return the playlistid
     */
    String getPlaylistId(Long tweetId);

    /**
     * Creates the since_id table
     */
    void createSinceIdTable();

    /**
     * Insert the id of the newest tweet from getMentionsTimeline endpoint into table. This only occurs once.
     *
     * @param since_id id of the newest tweet from getMentionsTimeline endpoint
     */
    void insertSinceId(long since_id);

    /**
     * Update the since_id
     *
     * @param since_id id of the newest tweet from getMentionsTimeline endpoint
     */
    void updateSinceId(long since_id);


    /**
     * Get the since_id
     *
     * @return since_id which is used to get the tweets greater than the since_id
     * (since_id is updated to the highest id from the getMentionsTimeline endpoint)
     */
    long getSinceId();

    /**
     * Creates the future_tweet table
     */
    void createFutureTweetTable();

    /**
     * Insert the id of the tweet from getMentionsTimeline endpoint into table.
     *
     * @param tweetId           id of the tweet from getMentionsTimeline endpoint that needs to be processed on a Friday
     * @param inReplyToStatusId id of the tweet from getMentionsTimeline endpoint that needs to be responded to
     */
    void insertFutureTweet(long tweetId, long inReplyToStatusId);

    /**
     * Delete all tweets in table
     */
    void deleteFutureTweets();

    /**
     * Gets the tweets that need to be processed on Friday
     *
     * @return map of future tweets to be processed
     */
    Map<Long, Long> getFutureTweets();
}
