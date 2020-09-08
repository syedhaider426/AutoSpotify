package com.spring.autospotify;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Twitter {
    private final twitter4j.Twitter twitter;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    JDBCUtil db = JDBCUtil.getInstance();

    public Twitter() throws IOException, SQLException, ClassNotFoundException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
                .setUser("autospotify426")
                .setOAuthConsumerKey(prop.getProperty("consumerKey"))
                .setOAuthConsumerSecret(prop.getProperty("consumerSecret"))
                .setOAuthAccessToken(prop.getProperty("accessToken"))
                .setOAuthAccessTokenSecret(prop.getProperty("accessTokenSecret"));
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter4j.Twitter twitter = tf.getInstance();
        this.twitter = twitter;
    }

    //348768375 - B&L
    //729066981077311488 - RiverBeats1
    //62786088 - DancingAstro
    public Map<Long,Long> getMentions() throws TwitterException {
        //key = tweetid
        //value inreplytostatusid
        Map<Long, Long> tweets = new HashMap<>();
        ResponseList<Status> responseList = twitter.timelines().getMentionsTimeline();
        long[] createdByList = {348768375L,729066981077311488L,62786088L};
        long max_id;
        for(int i = 0; i < responseList.size(); i++){
            Status stat = responseList.get(i);
            if(Arrays.asList(createdByList).contains(stat.getInReplyToUserId()) == false)
                replyTweet(stat.getInReplyToStatusId(),"You must call the bot on tweets made by TeamBAndL, RiverBeats1, or DancingAstro.");
            else {
                tweets.put(stat.getId(), stat.getInReplyToStatusId());
            }
        }
        return tweets;
    }

    public void replyTweet(long inReplyToStatusId, String tweet) throws TwitterException{
        StatusUpdate status = new StatusUpdate(tweet);
        status.setInReplyToStatusId(inReplyToStatusId);
        twitter.updateStatus(status);
    }


    // Get all the artists that are mentioned in the tweet
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
            else
                break;
        }
        // When parsing, need to start from index of 2

        return artistMap;
    }

    // Get the date of the tweet
    public LocalDateTime getStatusDate(long tweetid) throws TwitterException {
        Status stat = twitter.showStatus(tweetid);
        long date = stat.getCreatedAt().getTime();
        LocalDateTime statusDate = new Date(date).toLocalDate().atStartOfDay();
        return statusDate;
    }


}
