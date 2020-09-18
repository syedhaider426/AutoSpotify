package com.spring.autospotify;


import org.apache.tomcat.jni.Local;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws IOException {

        // Initialize the database
        JDBCUtil db = JDBCUtil.getInstance();
        db.createArtistTable();
        db.createPlaylistTweetTable();
        db.createSinceIdTable();

        // Create instance of Spotify
        Spotify spotify = new Spotify(db);

        //Create instance of Twitter
        Twitter twitter = new Twitter();

        //key = tweetid, value = inreplytostatusid
        Map<Long, Long> tweetIdList = twitter.getMentions();
        if (tweetIdList.size() < 0) {
            System.out.println("No mentions found");
            return;
        }
        for (Map.Entry<Long, Long> entry : tweetIdList.entrySet()) {
            Long tweetid = entry.getKey();
            System.out.println("Statusid: " + tweetid);
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
            if (artists.size() < 2) {
                System.out.println("No artists found");
                continue;
            }

            // Search for each artist in db or spotify api
            ArrayList<String> artistIdList = spotify.searchArtist(artists);
            //ArrayList<String> artistIdList = new ArrayList<>();
            if (artistIdList.size() <= 0) {
                System.out.println("No artists found");
                continue;
            }

            // Get releases based of the tweet date
            LocalDateTime tweetDate = twitter.getStatusDate(tweetid);
            ArrayList<String> albumReleases = spotify.getReleases(artistIdList, tweetDate);
            if (artistIdList.size() <= 0) {
                System.out.println("No new releases found");
                continue;
            }

            // Get each track to be added to Spotify
            ArrayList<String> releases = spotify.getAlbumTracks(albumReleases);
            if (artistIdList.size() <= 0) {
                System.out.println("Tracks for the requested albums were not found");
                continue;
            }

            DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            String newPlaylistId = spotify.createPlaylist("shayder426", "New Music for " + tweetDate.toLocalDate().format(format));
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
