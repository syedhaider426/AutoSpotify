package com.spring.autospotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws TwitterException, SQLException, ClassNotFoundException, ParseException, SpotifyWebApiException, IOException {
        //SpringApplication.run(AutoSpotifyApplication.class, args);
        System.out.println("RUNNING");
        JDBCUtil db = new JDBCUtil();
        db.init();
        Spotify spotify = new Spotify();
        spotify.searchArtist("Bonnie X Clyde","Bonnie");
    }

}
