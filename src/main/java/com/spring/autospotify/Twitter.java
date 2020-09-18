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
        this.twitter = tf.getInstance();
    }

    //348768375 - B&L
    //729066981077311488 - RiverBeats1
    //62786088 - DancingAstro
    //709746338376896513 - Electric Hawk
    public Map<Long, Long> getMentions() {
        Map<Long, Long> tweets = new LinkedHashMap<>();
        try {
            ResponseList<Status> responseList = twitter.timelines().getMentionsTimeline();
            long[] approvedUserIdList = {709746338376896513L, 348768375L, 729066981077311488L, 62786088L};
            Boolean found = false;
            for (Status stat : responseList) {
                Long inReplyToUserId = stat.getInReplyToUserId();
                for (Long approvedUserId : approvedUserIdList) {
                    if (approvedUserId.equals(inReplyToUserId)) {
                        Status stats = twitter.showStatus(stat.getInReplyToStatusId());
                        if (stats.getInReplyToUserId() == -1L) {    //indicates the top-most, parent tweet
                            tweets.put(stats.getId(), stat.getId());    //Tweet with artists, tweet that called bot
                            found = true;
                            break;
                        }
                    }
                }
            }
            return tweets;
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
        return tweets;
    }

    public void replyTweet(long inReplyToStatusId, String tweet) {
        try {
            Status stat = twitter.showStatus(inReplyToStatusId);
            StatusUpdate status = new StatusUpdate("@" + stat.getUser().getScreenName() + " " + tweet);
            status.inReplyToStatusId(inReplyToStatusId);
            twitter.updateStatus(status);
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
    }


    // Get all the artists that are mentioned in the tweet
    public ArrayList<String> getArtists(long tweetid) {
        ArrayList<String> artistList = new ArrayList<>();
        try {
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
                    for (String tempArtist : tempArtists) {
                        artistList.add(tempArtist.trim());
                    }
                } else if (artist.contains(" X ") && !(Arrays.asList(artistsWithX).contains(artist))) {
                    tempArtists = artist.split(" X ");
                    for (String tempArtist : tempArtists) {
                        artistList.add(tempArtist.trim());
                    }
                } else if (artist.contains("[R]")) {
                    tempArtists = artist.split("[R]");
                    for (String tempArtist : tempArtists) {
                        artistList.add(tempArtist.trim());
                        break;  //Only get the first artist
                    }
                } else if (artist.length() > 1) {
                    artistList.add(artist);
                } else  // This occurs when all artists have been parsed and there is an empty line afterwards
                    break;
            }
            // When parsing, need to start from index of 2

            return artistList;
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
        return artistList;
    }

    // Get the date of the tweet
    public LocalDateTime getStatusDate(long tweetid) {
        LocalDateTime statusDate = null;
        try {
            Status stat = twitter.showStatus(tweetid);
            long date = stat.getCreatedAt().getTime();
            statusDate = new Date(date).toLocalDate().atStartOfDay();
            return statusDate;
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
        return statusDate;
    }


}
