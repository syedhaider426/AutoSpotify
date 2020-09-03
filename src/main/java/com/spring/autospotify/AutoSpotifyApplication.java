package com.spring.autospotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import twitter4j.TwitterException;

import java.sql.SQLException;

//@SpringBootApplication
public class AutoSpotifyApplication {

    public static void main(String[] args) throws TwitterException, SQLException, ClassNotFoundException {
        //SpringApplication.run(AutoSpotifyApplication.class, args);
        System.out.println("RUNNING");
        JDBCUtil db = new JDBCUtil();
        db.init();
    }

}
