package com.spring.autospotify;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

/**
 * This application is intended to take a tweet where the autospotify426 bot
 * is mentioned and generate a Spotify playlist from the artists listed in the tweet and
 * get the tracks released during the week of the tweet .
 */
public class AutoSpotifyApplication {

    public static void main(String[] args) {

        // Initialize the database
        JDBCUtil db = new JDBCUtil();
        db.createArtistTable();
        db.createPlaylistTweetTable();
        db.createSinceIdTable();

        // Create instance of Spotify
        Spotify spotify = new Spotify(db);

        // Create instance of Twitter
        Twitter twitter = new Twitter();

        // Get the most recent mentions of bot offset by the since_id (newest id of the getMentionsTimeline endpoint)
        long since_id = db.getSinceId();
        Map<Long, Long> tweetIdList = twitter.getMentions(since_id);
        if (tweetIdList.size() == 0) {
            System.out.println("No mentions found");
            return;
        }
        if (since_id == 1L)
            db.insertSinceId(tweetIdList.get(tweetIdList.keySet().toArray()[0])); // no since_id - occurs when only once, when program is run for the first time ever
        else
            db.updateSinceId(tweetIdList.get(tweetIdList.keySet().toArray()[0])); //  one since_id exists

        // Loop through tweets and generate playlist per tweet
        for (Map.Entry<Long, Long> entry : tweetIdList.entrySet()) {
            Long tweetid = entry.getKey();
            Long inReplyToStatusId = entry.getValue();

            // If tweet exists, get track associated with it
            String playlistId = db.getPlaylistId(tweetid);
            if (playlistId.length() > 0) {
                System.out.println("Found the playlist link");
                twitter.replyTweet(inReplyToStatusId, "Poggers, this tweet was automated. Playlist is here at https://open.spotify.com/playlist/" + playlistId);
                continue;
            }

            // Gets tweet and parses it
            ArrayList<String> artists = twitter.getArtists(tweetid);
            if (artists.size() < 6) {
                System.out.println("No artists found");
                twitter.replyTweet(inReplyToStatusId, "There needs to be at least 6 artists in the tweet for playlist to be created.");
                continue;
            }

            // Search for each artist in db or spotify api
            ArrayList<String> artistIdList = spotify.searchArtist(artists);

            // ArrayList<String> artistIdList = new ArrayList<>();
            if (artistIdList.size() <= 0) {
                System.out.println("No artists found");
                twitter.replyTweet(inReplyToStatusId, "None of the artists in tweet were found on Spotify");
                continue;
            }

            // Get tweet details - User and Tweet Date
            Map<String, LocalDateTime> map = twitter.getTweetDetails(tweetid);
            Map.Entry<String, LocalDateTime> entry2 = map.entrySet().iterator().next();
            String user = entry2.getKey(); // author
            LocalDateTime tweetDate = entry2.getValue(); // tweet date

            // Get the songs/albums/eps released for the week of the tweet
            ArrayList<String> albumReleases = spotify.getReleases(artistIdList, tweetDate);
            if (artistIdList.size() <= 0) {
                System.out.println("No new releases found");
                twitter.replyTweet(inReplyToStatusId, "None of the artists mentioned in the tweet has released music the week of this tweet.");
                continue;
            }

            // Get each track from album releases and that will be added to the playlist
            ArrayList<String> releases = spotify.getAlbumTracks(albumReleases);
            if (artistIdList.size() <= 0) {
                System.out.println("Tracks for the requested albums were not found");
                twitter.replyTweet(inReplyToStatusId, "Spotify had an issue finding music for the artists listed.");
                continue;
            }

            // User who creates the playlist
            String userid = "shayder426";

            // Date of tweet
            DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            String playlistName = user + " - New Music for " + tweetDate.toLocalDate().format(format);

            // Create playlist
            String newPlaylistId = spotify.createPlaylist(userid, playlistName);
            if (newPlaylistId == null) {
                twitter.replyTweet(inReplyToStatusId, "Issue creating playlist. Please try again later.");
                continue;
            }
            // Store playlist and the tweet they are related to
            db.insertPlaylist_Tweet(tweetid, newPlaylistId);

            // Add songs to playlist
            Boolean songsAdded = false;
            songsAdded = spotify.addSongsToPlaylist(newPlaylistId, releases);
            if (songsAdded) {
                System.out.println("Songs were added successfully");
                twitter.replyTweet(inReplyToStatusId, "Poggers. This tweet was automated. Playlist is here at https://open.spotify.com/playlist/" + newPlaylistId);
            } else {
                System.out.println("ERROR. Songs not added");
                twitter.replyTweet(inReplyToStatusId, "Unable to add songs to playlist. Please try again later.");
            }

        }
    }

}
