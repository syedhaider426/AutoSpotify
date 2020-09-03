package com.spring.autospotify;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.Properties;

public class Twitter {
    private final twitter4j.Twitter twitter;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    String consumerKey = prop.getProperty("consumerKey");
    String consumerSecret = prop.getProperty("consumerSecret");
    String accessToken = prop.getProperty("accessToken");
    String accessTokenSecret = prop.getProperty("accessTokenSecret");

    public Twitter() throws IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter4j.Twitter twitter = tf.getInstance();
        this.twitter = twitter;
    }

    public void createTweet(String tweet) throws TwitterException {
        Status status = twitter.updateStatus(tweet);
        System.out.println("Succesfully updated the status to [" + status.getText());
    }
    public void replyTweet(String tweet, Long replyId) throws TwitterException{
        StatusUpdate statusUpdate = new StatusUpdate(tweet);
        statusUpdate.setInReplyToStatusId(replyId);
        Status status = twitter.updateStatus(statusUpdate);
        System.out.println("Replied to " + replyId);
    }

}
