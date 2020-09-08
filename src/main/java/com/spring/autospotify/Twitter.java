package com.spring.autospotify;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

public class Twitter {
    private twitter4j.Twitter twitter;

    public Twitter() {
        try {
            GetPropertyValues properties = new GetPropertyValues();
            Properties prop = properties.getPropValues();
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //348768375 - B&L
    //729066981077311488 - RiverBeats1
    //62786088 - DancingAstro
    public Map<Long, Long> getMentions() throws TwitterException {
        //key = tweetid
        //value inreplytostatusid
        Map<Long, Long> tweets = new HashMap<>();
        ResponseList<Status> responseList = twitter.timelines().getMentionsTimeline();
        long[] createdByList = {348768375L, 729066981077311488L, 62786088L};
        long max_id;
        for (int i = 0; i < responseList.size(); i++) {
            Status stat = responseList.get(i);
            if (Arrays.asList(createdByList).contains(stat.getInReplyToUserId()) == false)
                replyTweet(stat.getInReplyToStatusId(), "You must call the bot on tweets made by TeamBAndL, RiverBeats1, or DancingAstro.");
            else {
                tweets.put(stat.getId(), stat.getInReplyToStatusId());
            }
        }
        return tweets;
    }

    public void replyTweet(long inReplyToStatusId, String tweet) throws TwitterException {
        StatusUpdate status = new StatusUpdate(tweet);
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
            String artist = artists[x].toUpperCase();
            if (artist.contains("+")) {
                tempArtists = artist.split("\\+");
                for (String tempArtist: tempArtists) {
                    artistList.add(tempArtist);
                }
            } else if (artist.contains(" X ") && !(Arrays.asList(artistsWithX).contains(artist))) {
                tempArtists = artist.split(" X ");
                for (String tempArtist: tempArtists) {
                    artistList.add(tempArtist);
                }
            } else if (artistsLength > 1) {
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
