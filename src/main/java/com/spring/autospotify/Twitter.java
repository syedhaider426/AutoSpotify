package com.spring.autospotify;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

public class Twitter {
    private twitter4j.Twitter twitter;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();

    public Twitter() throws IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
                .setUser("autospotify426")
                .setOAuthConsumerKey(prop.getProperty("consumerKey"))
                .setOAuthConsumerSecret(prop.getProperty("consumerSecret"))
                .setOAuthAccessToken(prop.getProperty("accessToken"))
                .setOAuthAccessTokenSecret(prop.getProperty("accessTokenSecret"));
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
        this.twitter = twitter;
    }

    //348768375 - B&L
    //729066981077311488 - RiverBeats1
    //62786088 - DancingAstro
    public Map<Long, Long> getMentions() throws TwitterException {
        //key = tweetid
        //value inreplytostatusid
        Map<Long, Long> tweets = new LinkedHashMap<>();
        ResponseList<Status> responseList = twitter.timelines().getMentionsTimeline();

        long[] approvedUserIdList = {348768375L, 729066981077311488L, 62786088L};
        for (Status stat : responseList) {
            for (Long approvedUserId : approvedUserIdList) {
                Long inReplyToUserId = stat.getInReplyToUserId();
                if (!approvedUserId.equals(inReplyToUserId))
                    System.out.print("");
                    //replyTweet(stat.getInReplyToStatusId(), "You must call the bot on tweets made by TeamBAndL, RiverBeats1, or DancingAstro.");
                else if (!inReplyToUserId.equals(-1L)) {
                    System.out.println("Id - 2: " + stat.getInReplyToStatusId());
                    System.out.println("StatusID - 2:" + stat.getId());
                    tweets.put(stat.getInReplyToStatusId(), stat.getId());
                }
            }
        }
        return tweets;
    }

    public void replyTweet(long inReplyToStatusId, String tweet) throws TwitterException {
        StatusUpdate status = new StatusUpdate(tweet);
        System.out.println("inreplytostatusid - " + inReplyToStatusId);
        status.setInReplyToStatusId(inReplyToStatusId);
        twitter.updateStatus(status);
    }


    // Get all the artists that are mentioned in the tweet
    public ArrayList<String> getArtists(long tweetid) throws TwitterException {
        ArrayList<String> artistList = new ArrayList<>();
        String[] tempArtists;

        String status = twitter.showStatus(tweetid).getText();
        String[] artists = status.split("\n");
        String[] artistsWithX = {"BONNIE X CLYDE", "LIL NAS X", "SOB X RBE"};
        int artistsLength = artists.length;

        for (int x = 2; x < artistsLength; x++) {
            tempArtists = null;
            String artist = artists[x].toUpperCase().trim();
            System.out.println(artist);
            if (artist.contains("+")) {
                tempArtists = artist.split("\\+");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist);
                }
            } else if (artist.contains(" X ") && !(Arrays.asList(artistsWithX).contains(artist))) {
                tempArtists = artist.split(" X ");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist);
                }
            } else if (artist.length() > 1) {
                artistList.add(artist);
            } else  // This occurs when all artists have been parsed and there is an empty line afterwards
                break;
        }
        // When parsing, need to start from index of 2

        return artistList;
    }

    // Get the date of the tweet
    public LocalDateTime getStatusDate(long tweetid) throws TwitterException {
        Status stat = twitter.showStatus(tweetid);
        long date = stat.getCreatedAt().getTime();
        LocalDateTime statusDate = new Date(date).toLocalDate().atStartOfDay();
        return statusDate;
    }


}
