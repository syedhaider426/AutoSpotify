package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws TwitterException, SQLException, ClassNotFoundException, ParseException, SpotifyWebApiException, IOException {
        String tweetid = "";
        // Initialize the database
        JDBCUtil db = new JDBCUtil();
        db.init();

        // Create instance of Spotify
        Spotify spotify = new Spotify();
        String artistId= "";

        //Parse tweet
        //Loop through tweet
        //Add to hashMap
        //Return hashmap

        int artistLength = 1;

        ArrayList<String> artistIdList = new ArrayList<>();
        // Search for the artists and see if they exist in DB or in Spotify
        // O(n)
        for(int x = 0; x < artistLength; x++) {
            String[] artistInfo = spotify.searchArtist("Bonnie X Clyde", "Bonnie");
            artistIdList.add(artistInfo[1]);
        }

        ArrayList<String> releases = new ArrayList<>();
        // Get new releases for the artist
        // *NOTE* - Releases should be all releases for all artists
        // O(n^2)
        for(int x = 0; x < artistIdList.size(); x++) {
            ArrayList<String> artistReleases = spotify.getNewReleases(artistIdList.get(x));
            releases.addAll(artistReleases);
        }
        // Store list of new tracks and the tweet they are related to
        db.insertUriTweet(releases,tweetid);
        // Create playlist

        // Add songs to playlist
        // Check for duplicates
        // Send user to playlist
    }

}
