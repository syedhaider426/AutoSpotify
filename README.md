# AutoSpotify
Autospotify is a Twitter bot (https://twitter.com/autospotify426) that converts tweets from music record labels into Spotify playlists. Each tweet consists of a list of artists who will be releasing music on the following Friday. The bot parses the tweet to get a list of artists, checks the date of the tweet, and finds the new songs released within the week of the date for the particular artist via Spotify's API. Once all songs have been found, a playlist for the specific tweet is created and stored in the configured user's account. The bot will run once a day, checking for any mentions from other users on Twitter. The bot will generate playlist for the tweets they are mentioned in.

# Installation
1) Clone the repository or download the zip file from the 'Releases' menu
2) Navigate to secrets.properties where all the required fields must be configured. Here are the following keys that need to be configured.

    **Spotify API**
    <ul>
      <li>spotifyClientId: Unique identifier for Spotify Application</li>
      <li>spotifyClientSecret: Key that is passed in secure calls to the Spotify Accounts and Web API services</li>
      <li>refreshToken: Token for account that will store the playlists</li>
    </ul>

    **Twitter API**
    <ul>
      <li>consumerKey: Twitter's API Key</li>
      <li>consumerSecret: Twitter's API Secret</li>
      <li>accessToken: Access token that is necessary to make secure calls to Twitter's API Services</li>
      <li>accessTokenSecret: Access token secret that is necessary to make secure calls to Twitter's API Services</li>
    </ul>

    **Database - User can choose to use a Postgres or MongoDB database**
    <ul>
      <li>username: Username for PostgreSQL database</li>
      <li>password: Password for PostgreSQL database</li>
      <li>url: URL for PostgreSQL database or MongoDB Database</li>
      <li>database: MongoDB Database name</li>
    </ul>

3) After all the configuration properties have been set, application can now be run.
    <ul>
      <li>The main class is AutoSpotifyApplication. This is what Java will run in the local environment. This is useful for testing the application works properly locally.</li>
      <li>The class that is run by AWS Lambda is HandlerStream. If one decides to deploy the application on AWS Lambda, additional instruction can be found at https://docs.aws.amazon.com/toolkit-for-eclipse/v1/user-guide/lambda-tutorial.html</li>
  </ul>
  
4) All dependencies have already been configured in pom.xml.

# Built With
<ul>
<li>Java</li>
<li>MongoDB & PostgreSQL</li>
<li>AWS Lambda - Serverless function used to run the bot periodically</li>
<li>AWS Cloudwatch - The event that controls the schedule for the AWS Lambda function</li>
</ul>

# Authors
Syed Haider
