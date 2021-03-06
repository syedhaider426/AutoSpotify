package com.autospotify;

import com.autospotify.apis.Spotify;
import com.autospotify.apis.Twitter;
import com.autospotify.database.Database;
import com.autospotify.database.MongoDB;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

/**
 * This application is intended to take a tweet where the autospotify426 bot
 * is mentioned and generate a Spotify playlist from the artists listed in the tweet,
 * getting the tracks released during the week of the tweet .
 */
public class AutoSpotifyApplication {

    public static void main(String[] args) throws IOException {

        // Initialize the database
        Database db = new MongoDB();

        // Only needs to run when program is first run
        db.initialize();

        // Create instance of Spotify
        Spotify spotify = new Spotify(db);

        // User who creates the playlist
        String userid = "shayder426";

        // Create instance of Twitter
        Twitter twitter = new Twitter(db);

        // Get the most recent mentions of bot offset by the since_id (newest id of the getMentionsTimeline endpoint)
        Map<Long, Long> tweetIdList = twitter.getMentions(db.getSinceId());

        Map<Long, Long> futureTweets = null;
        if (LocalDate.now().getDayOfWeek().getValue() == DayOfWeek.FRIDAY.getValue()) {
            futureTweets = db.getFutureTweets();
            // If there are any tweets stored in future_tweet that can be processed, add to list
            if (futureTweets != null && futureTweets.size() > 0) {
                for (Map.Entry<Long, Long> entry : futureTweets.entrySet()) {
                    tweetIdList.put(entry.getKey(), entry.getValue());
                }
                db.deleteFutureTweets();
            }
        }

        // If bot was not mentioned in any tweets, return
        if (tweetIdList.size() == 0) {
            System.out.println("No mentions found");
            return;
        }

        // Loop through tweets and generate playlist per tweet
        for (Map.Entry<Long, Long> entry : tweetIdList.entrySet()) {
            Long tweetId = entry.getKey();
            Long inReplyToStatusId = entry.getValue();


            // Get tweet details - User and Tweet Date
            Map.Entry<String, LocalDateTime> entry2 = twitter.getTweetDetails(tweetId).entrySet().iterator().next();
            LocalDateTime tweetDate = entry2.getValue(); // tweet date

            /* For the week of the tweet was posted at, the Friday must come before the current date to be processed
             * Else, it will be processed on the next upcoming Friday
             */
            LocalDate tweetStartDate = tweetDate.toLocalDate();
            DayOfWeek dayOfWeek = tweetStartDate.getDayOfWeek(); //starts at 0
            LocalDate tweetEndDate = tweetStartDate.plusDays(DayOfWeek.FRIDAY.getValue() - dayOfWeek.getValue());
            if (tweetEndDate.isAfter(LocalDate.now())) {
                System.out.println("This playlist will be generated around 12:30 AM EST on " + tweetEndDate);
                db.insertFutureTweet(tweetId, inReplyToStatusId);
                twitter.replyTweet(inReplyToStatusId, "This playlist will be generated around 12:30 AM EST on " + tweetEndDate);
                continue;
            }

            // If tweet exists, get track associated with it
            String playlistId = db.getPlaylistId(tweetId);
            if (playlistId.length() > 0) {
                System.out.println("Found the playlist link");
                twitter.replyTweet(inReplyToStatusId, "This tweet was automated. Playlist is here at https://open.spotify.com/playlist/" + playlistId);
                continue;
            }

            // Gets tweet and parses it
            ArrayList<String> artists = twitter.getArtists(tweetId);
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

            // Get the songs/albums/eps released for the week of the tweet
            ArrayList<String> albumReleases = spotify.getReleases(artistIdList, tweetDate);
            if (artistIdList.size() <= 0) {
                System.out.println("No new releases found");
                twitter.replyTweet(inReplyToStatusId, "None of the artists mentioned in the tweet has released music the week of this tweet.");
                continue;
            }

            // Get each track from album releases and that will be added to the playlist
            ArrayList<String> tracks = spotify.getAlbumTracks(albumReleases, artistIdList);
            if (artistIdList.size() <= 0) {
                System.out.println("Tracks for the requested albums were not found");
                twitter.replyTweet(inReplyToStatusId, "Spotify had an issue finding music for the artists listed.");
                continue;
            }

            // Date of tweet
            DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            String playlistName = entry2.getKey() + " - New Music for " + tweetDate.toLocalDate().format(format);

            // Create playlist
            String newPlaylistId = spotify.createPlaylist(userid, playlistName);
            if (newPlaylistId == null) {
                twitter.replyTweet(inReplyToStatusId, "Issue creating playlist. Please try again later.");
                continue;
            }
            System.out.println("New playlist added: " + newPlaylistId);

            // Store playlist and the tweet they are related to
            db.insertPlaylist_Tweet(tweetId, newPlaylistId);

            // Add tracks to playlist
            spotify.addTracksToPlaylist(twitter, inReplyToStatusId, newPlaylistId, tracks);
        }
    }
}
