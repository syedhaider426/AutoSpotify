package com.spring.autospotify;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Twitter class used to access twitter object and twitter api methods
 * This class is primarily used to get the bot mentions, parse the tweets, and reply to tweets
 */
public class Twitter {
    private static twitter4j.Twitter twitter;
    private static Database db;

    /**
     * Constructor for Twitter object
     */
    public Twitter(Database db) {
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
            twitter = tf.getInstance();
            this.db = db;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //348768375 - B&L
    //729066981077311488 - RiverBeats1
    //62786088 - DancingAstro
    //709746338376896513 - Electric Hawk

    /**
     * Get the mentions for autospotify426 and checks to see if the account that
     * mentioned the bot, is doing so in a response to a tweet from one of the 4 listed accounts
     *
     * @param since_id Get mentions greater than the since_id
     * @return map that contains the tweet to reply to (the parent tweet) and the tweet
     * in which the bot was called
     */
    public Map<Long, Long> getMentions(long since_id) {
        Map<Long, Long> tweets = new LinkedHashMap<>();
        try {
            Paging sinceId = new Paging(since_id);
            ResponseList<Status> responseList = twitter.timelines().getMentionsTimeline(sinceId);
            long[] approvedUserIdList = {709746338376896513L, 348768375L, 729066981077311488L, 62786088L};
            boolean found = false;
            if (responseList.size() > 0) {
                db.updateSinceId(responseList.get(0).getId());
                for (Status stat : responseList) {
                    Long inReplyToUserId = stat.getInReplyToUserId();
                    System.out.println("Status Id: " + stat.getId());
                    for (Long approvedUserId : approvedUserIdList) {
                        //if (approvedUserId.equals(inReplyToUserId)) {
                        try {
                            Status stats = twitter.showStatus(stat.getInReplyToStatusId());
                            if (stats.getInReplyToUserId() == -1L) {    //indicates the top-most, parent
                                tweets.put(stats.getId(), stat.getId());    //Tweet with artists, tweet that called bot
                                found = true;
                                break;
                            }
                        } catch (TwitterException ex) {
                            ex.printStackTrace();
                        }
                        //}
                    }

                }
            }
            return tweets;
        } catch (TwitterException ex) {
            ex.printStackTrace();
            System.out.println("Unable to get mentions");
        }
        return tweets;
    }

    /**
     * Replies to user's tweet based on statusid with specific tweet in params
     *
     * @param inReplyToStatusId Tweet to reply to
     * @param tweet             The content of the tweet
     */
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

    /**
     * Get all the artists that are mentioned in the tweet
     *
     * @param tweetid - id of the tweet that has all the artists
     * @return arraylist of artist's name
     */
    public ArrayList<String> getArtists(long tweetid) {
        ArrayList<String> artistList = new ArrayList<>();
        String status = "";
        String[] tempArtists;   // Used to keep tracks of artists in tweet that have an X, +, [R], or FT.
        try {
            status = twitter.showStatus(tweetid).getText();
            // Gets the text of the parent tweet (parent indicates the tweet that the bot is called upon)
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
        // Each tweet has artists listed, separated by a new line
        String[] artists = status.split("\n");

        /* Some artists have X in their name, so check the list for each artist
         * if it does. If it is in that list, do not split on the X.
         */
        String[] artistsWithX = {"BONNIE X CLYDE", "LIL NAS X", "SOB X RBE"};
        int artistsLength = artists.length;
        // Start at index 2 because first two lines are usually "New Music" and a blank separator line
        for (int x = 2; x < artistsLength; x++) {
            tempArtists = null; // clear values in array
            String artist = artists[x].toUpperCase();
            // Sanitize data based off if they contain +, ' x ', [R], or FT.
            // If none, just use the artist name
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
            } else if (artist.contains("FT.")) {
                tempArtists = artist.split("FT.");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist.trim());
                }
            } else if (artist.contains("&")) {
                tempArtists = artist.split("&");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist.trim());
                }
            } else if (artist.contains(" EP")) {
                tempArtists = artist.split("EP");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist.trim());
                }
            } else if (artist.contains(" LP")) {
                tempArtists = artist.split("LP");
                for (String tempArtist : tempArtists) {
                    artistList.add(tempArtist.trim());
                }
            } else if (artist.length() > 1) {
                artistList.add(artist);
            } else // This occurs when all artists have been parsed and there is an empty line afterwards
                break;
        }
        return artistList;

    }

    /**
     * Get the date of the tweet and author of the parent tweet
     *
     * @param tweetid id of the tweet
     * @return an object that has the tweet's screen name and date of the tweet
     */
    public Map<String, LocalDateTime> getTweetDetails(long tweetid) {
        Map<String, LocalDateTime> map = new HashMap<>();
        try {
            Status stat = twitter.showStatus(tweetid);  // Gets the tweet
            long date = stat.getCreatedAt().getTime();
            LocalDateTime statusDate = new Date(date).toLocalDate().atStartOfDay();
            map.put(stat.getUser().getScreenName(), statusDate);
            return map;
        } catch (TwitterException ex) {
            ex.printStackTrace();
        }
        return map;
    }
}