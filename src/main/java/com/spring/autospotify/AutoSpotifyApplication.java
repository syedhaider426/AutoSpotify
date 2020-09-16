package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.apache.tomcat.jni.Local;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws SQLException, ParseException, SpotifyWebApiException, TwitterException, IOException {

        // Initialize the database
        JDBCUtil db = JDBCUtil.getInstance();
        db.createArtistTable();
        db.createUriTweetTable();

        // Create instance of Spotify
        Spotify spotify = new Spotify(db);

        Twitter twitter = new Twitter();
        //key = tweetid, value = inreplytostatusid
        Map<Long, Long> tweetIdList = twitter.getMentions();
        if (tweetIdList.size() < 0) {
            System.out.println("No mentions found");
            return;
        }
        for (Map.Entry<Long, Long> entry : tweetIdList.entrySet()) {

            Long tweetid = entry.getKey();
            Long inReplyToStatusId = entry.getValue();
            // If tweet exists, get track associated with it
            ArrayList<String> uriList = db.getTracks(tweetid);
            if (uriList.size() > 0) {
                System.out.println("Found Tweet in database");
                Boolean songsAdded = false;
                String playlistId = "19Cg0aKbM7UtUfdx873CEA";
                songsAdded = spotify.addSongsToPlaylist(playlistId, uriList);
                if (songsAdded) {
                    System.out.println("Songs were added successfully");
                    //twitter.replyTweet(inReplyToStatusId, "Playlist is here at https://open.spotify.com/playlist/" + playlistId);
                    continue;
                } else {
                    System.out.println("ERROR. Songs not added");
                    //twitter.replyTweet(inReplyToStatusId, "Unable to add songs to playlist. Please try again later.");
                    continue;
                }
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
            }/*
            // Get each track to be added to Spotify
            ArrayList<String> releases = spotify.getAlbumTracks(albumReleases);
            if (artistIdList.size() <= 0) {
                System.out.println("Tracks for the requested albums were not found");
                continue;
            }

            // Store list of new tracks and the tweet they are related to
            //db.insertUriTweet(releases, tweetid);

            DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            String playlistId = spotify.createPlaylist("", "New Music for " + tweetDate.toLocalDate().format(format));
            System.out.println(tweetDate.toLocalDate().format(format));
            // Add songs to playlist
            Boolean songsAdded = false;
            songsAdded = spotify.addSongsToPlaylist("19Cg0aKbM7UtUfdx873CEA", releases);
            if (songsAdded) {
                System.out.println("Songs were added successfully");
                //twitter.replyTweet(tweetid,"Poggers. This tweet was automated. Playlist is here at https://open.spotify.com/playlist/" + playlistId );
            } else {
                System.out.println("ERROR. Songs not added");
                //twitter.replyTweet(tweetid,"Unable to add songs to playlist. Please try again later.");
            }*/
        }

    }

}
