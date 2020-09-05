package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.apache.tomcat.jni.Local;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws TwitterException, SQLException, ClassNotFoundException, ParseException, SpotifyWebApiException, IOException {
        String tweetid = "1234";
        // Initialize the database
        JDBCUtil db = new JDBCUtil();
        db.createArtistTable();
        db.createUriTweetTable();
        // Create instance of Spotify
        Spotify spotify = new Spotify();

        ArrayList<String> artistList = new ArrayList<>();
        Map<String,String> artists = new HashMap<>();
        artists.put("Excision","Excision");
        artists.put("Virthual Riot","Virtuahl Riot");
        artists.put("Kompany","Kompany");

        // Search for maps
        ArrayList<String> artistIdList = spotify.searchArtist(artists);

        ArrayList<String> albumReleases = new ArrayList<>();
        // Get new releases for the artist
        for (int x = 0; x < artistIdList.size(); x++) {
            ArrayList<String> artistReleases = spotify.getNewReleases(artistIdList.get(x));
            albumReleases.addAll(artistReleases);
        }
        for(int x = 0; x < albumReleases.size(); x++){
            System.out.println("Album Release" + albumReleases.get(x));
        }
        ArrayList<String> releases = spotify.getAlbumTracks(albumReleases);

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
    }

}
