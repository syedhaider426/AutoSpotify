package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws TwitterException, SQLException, ClassNotFoundException, ParseException, SpotifyWebApiException, IOException {
        //SpringApplication.run(AutoSpotifyApplication.class, args);
        System.out.println("RUNNING");
        String tweetid = "";
        JDBCUtil db = new JDBCUtil();
        db.init();
        Spotify spotify = new Spotify();
        String[] artistInfo = spotify.searchArtist("Bonnie X Clyde","Bonnie");
        String artist = "";
        String spotifyId = "";
        if(artistInfo.length > 0) {
            artist = artistInfo[0];
            spotifyId = artistInfo[1];
        }
        else{
            return;
        }
        ArrayList<String> releases = spotify.getNewReleases(spotifyId);
        db.insertUriTweet(releases,tweetid);

    }

}
