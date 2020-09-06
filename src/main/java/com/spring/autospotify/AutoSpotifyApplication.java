package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.apache.tomcat.jni.Local;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, ParseException, SpotifyWebApiException, IOException, TwitterException {


        // Initialize the database
        JDBCUtil db = new JDBCUtil();
        db.createArtistTable();
        db.createUriTweetTable();
        // Create instance of Spotify
        Spotify spotify = new Spotify();

        Long tweetid = 1222957785454759936L;
        Twitter twitter = new Twitter();
        // Gets tweet and parses it
        Map<String, String> artists = twitter.getArtists(tweetid);
        if (artists.size() < 6) {
            System.out.println("No artists found");
            return;
        }
/*
        // Search for each artist in db or spotify api
        ArrayList<String> artistIdList = spotify.searchArtist(artists);
        if(artistIdList.size() <= 0){
            System.out.println("No artists found");
            return;
        }
        // Get releases based of the tweet date
        LocalDateTime tweetDate = twitter.getStatusDate(tweetid);
        ArrayList<String> albumReleases = spotify.getReleases(artistIdList,tweetDate);
        if(artistIdList.size() <= 0){
            System.out.println("No new releases found");
            return;
        }
        // Get each track to be added to Spotify
        ArrayList<String> releases = spotify.getAlbumTracks(albumReleases);
        if(artistIdList.size() <= 0){
            System.out.println("Tracks for the requests albums were not found");
            return;
        }

        // Store list of new tracks and the tweet they are related to
        db.insertUriTweet(releases,tweetid);

        // Add songs to playlist
        Boolean songsAdded = false;
        songsAdded = spotify.addSongsToPlaylist("19Cg0aKbM7UtUfdx873CEA", releases);
        if (songsAdded)
            System.out.println("Songs were added successfully");
        else {
            System.out.println("ERROR. Songs not added");
            return;
        }
        // Send user to playlist
*/

    }

}
