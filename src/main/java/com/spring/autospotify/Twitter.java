package com.spring.autospotify;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Twitter {
    private final twitter4j.Twitter twitter;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    public Twitter() throws IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
                .setOAuthConsumerKey(prop.getProperty("consumerKey"))
                .setOAuthConsumerSecret(prop.getProperty("consumerSecret"))
                .setOAuthAccessToken(prop.getProperty("accessToken"))
                .setOAuthAccessTokenSecret(prop.getProperty("accessTokenSecret"));
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter4j.Twitter twitter = tf.getInstance();
        this.twitter = twitter;
    }


    public Map<String,String> getArtists(long tweetid) throws TwitterException {
        String status = twitter.showStatus(tweetid).getText().toString();
        String[] artists = status.split("\n");
        String[] tempArtists;
        Map<String,String> artistMap = new HashMap<>();
        for (int x = 2; x < artists.length; x++) {
            tempArtists = null;
            String artist = artists[x].toUpperCase();
            if (artist.contains("+")) {
                tempArtists = artist.split("\\+");
                for(int y = 0; y < tempArtists.length; y++){
                    System.out.println("Added " + tempArtists[y] + " to map");
                    artistMap.put(tempArtists[y],tempArtists[y]);
                }
            }
            else if (artist.contains(" X ")) {
                tempArtists = artist.split(" X ");
                for(int y = 0; y < tempArtists.length; y++){
                    System.out.println("Added " + tempArtists[y] + " to map");
                    artistMap.put(tempArtists[y],artist);
                }
            }
            else if(artist.length() > 1){
                System.out.println("Added " + artist + " to map");
                artistMap.put(artist, artist);
            }
        }
        // When parsing, need to start from index of 2

        return artistMap;
    }

    public LocalDateTime getStatusDate(long tweetid) throws TwitterException {
        Status stat = twitter.showStatus(tweetid);
        long date = stat.getCreatedAt().getTime();
        LocalDateTime statusDate = new Date(date).toLocalDate().atStartOfDay();
        return statusDate;
    }


}
