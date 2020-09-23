package com.spring.autospotify;

import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.special.SnapshotResult;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.albums.GetAlbumsTracksRequest;
import com.wrapper.spotify.requests.data.artists.GetArtistsAlbumsRequest;
import com.wrapper.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.CreatePlaylistRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchArtistsRequest;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.hc.core5.http.ParseException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
/**
 * Spotify class is used to interact with spotify object and methods in order to create a playlist
 * and add songs to the playlist
 */
public class Spotify {
    private SpotifyApi spotifyApi;
    private JDBCUtil db;

    /**
     * Constructor for Spotify object
     *
     * @param db JDBC Postgresql db to call any database methods
     */
    public Spotify(JDBCUtil db) {
        Properties prop;
        try {
            // Get secret properties
            GetPropertyValues properties = new GetPropertyValues();
            prop = properties.getPropValues();
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(prop.getProperty("spotifyClientId"))
                    .setClientSecret(prop.getProperty("spotifyClientSecret"))
                    .setRefreshToken(prop.getProperty("refreshToken"))
                    .build();
            this.db = db;
            this.spotifyApi = spotifyApi;
        } catch (IOException ex) { //IOException throw by Prop
            ex.printStackTrace();
        }
    }

    /**
     * @param artistList List of artist names retrieved from tweet
     * @return list of spotifyids for each artist
     */
    public ArrayList<String> searchArtist(ArrayList<String> artistList) {
        System.out.println("Searching artists");
        ArrayList<String> artists = new ArrayList<>();
        String spotifyId;
        for (String artist : artistList) {
            // Checks the database for the artist to see if artist and corresponding spotify id exists
            spotifyId = db.getSpotifyID(artist);
            if (spotifyId.length() > 0) {
                artists.add(spotifyId);
                continue;
            }
            // Sets access token for Spotify object
            this.spotifyApi = setToken();

            // Builds request to search for artist based on artist name
            SearchArtistsRequest searchArtistsRequest = spotifyApi.searchArtists(artist).limit(10).build();
            try {
                final Paging<Artist> artistPaging = searchArtistsRequest.execute();
                Artist[] artistArr = artistPaging.getItems();
                // If no artists are found, check the next artist in ArrayList
                if (artistArr.length == 0) {
                    System.out.println("No artist found with name: " + artist);
                    continue;
                }
                for (Artist value : artistArr) {
                    // Spotify names are stored case-insensitive
                    String spotifyName = value.getName().toUpperCase();

                    // Spotify id for artist
                    String id = value.getId();

                    /* LevenshteinDistance is used to calculate how many characters is needed to get from source
                     * name to target name */
                    LevenshteinDistance l = new LevenshteinDistance();
                    int originalLeven = l.apply(artist, spotifyName);

                    /* If the spotify name passed into request matches with the name in the tweet or closesly matches it
                     * add to database and to arraylist */
                    if (artist.equals(spotifyName) || originalLeven <= 2) {
                        if ((originalLeven > 0 && originalLeven <= 2)) {
                            db.insertArtist(artist, id);
                        }
                        db.insertArtist(spotifyName, id);
                        artists.add(id);
                        break;
                    }
                }

            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Ending search for artists");
        return artists;
    }

    /**
     * Get the releases based off the date
     *
     * @param artistIdList List of artists to iterate through to check if they release any music on specified date
     * @param tweetDate    date of parent tweet (that contains artists)
     * @return list of spotify album ids
     */
    public ArrayList<String> getReleases(ArrayList<String> artistIdList, LocalDateTime tweetDate) {
        System.out.println("Loading releases");
        ArrayList<String> spotifyIdList = new ArrayList<>();
        int counter = 0;
        for (int x = 0; x < artistIdList.size(); x++) {
            boolean found = false;
            this.spotifyApi = setToken();
            System.out.println("Checking artist: " + artistIdList.get(x));
            // Build request to get albums for artist
            // Counter will either be 0 or 50 depending on if songs were found in first loop
            GetArtistsAlbumsRequest getArtistsAlbumsRequest = spotifyApi.getArtistsAlbums(artistIdList.get(x))
                    .limit(50)
                    .market(CountryCode.US)
                    .offset(counter)
                    .build();
            try {
                // Execute request to get albums for artist
                final Paging<AlbumSimplified> albums = getArtistsAlbumsRequest.execute();
                AlbumSimplified[] items = albums.getItems();
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (AlbumSimplified item : items) {
                    try {
                        LocalDateTime releaseDate = LocalDate.parse(item.getReleaseDate(), format).atStartOfDay();

                        // Duration between tweet date and release date of song
                        long d1 = Duration.between(tweetDate, releaseDate).toDays();

                        // If duration is less than two weeks, then add song album id to list
                        if (d1 <= 9 && d1 >= -9) {
                            spotifyIdList.add(item.getId());
                            found = true;
                            counter = 0;
                        }
                    } catch (DateTimeParseException ex) {   // This occurs when Spotify stores the date with just a year i.e 2015
                        continue;
                    }
                }
                // If song was found, go to next artist
                if (found) {
                    System.out.println("Found song");
                    continue;
                }
                // If song was not found in the first 50 songs, check the next 50 songs
                else if (!found && counter == 0 || (!found && counter == 50 && x == artistIdList.size() - 1)) {
                    System.out.println("No song found. Checking next 50");
                    x--;    //check the same artist
                    counter += 50;
                }
                // If song was not found within the first 100 songs read, then check the next artist
                else if (!found && counter == 50 && x != artistIdList.size() - 1) {
                    System.out.println("No song found. Going to next artist");
                    counter = 0;
                }
            } catch (IOException | SpotifyWebApiException | ParseException | DateTimeParseException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Ending releases");
        return spotifyIdList;
    }

    /**
     * Get the tracks based off the list of album releases
     *
     * @param albumReleases list of spotifyuris of type: album, single, ep, appears_on
     * @return list of tracks for each album to be added to playlist
     */
    public ArrayList<String> getAlbumTracks(ArrayList<String> albumReleases) {
        System.out.println("Loading tracks");
        ArrayList<String> releases = new ArrayList<>();
        Map<String, String> releasesMap = new HashMap<>();
        try {
            for (String albumRelease : albumReleases) {
                // Build request to get album tracks
                GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi.getAlbumsTracks(albumRelease).build();

                // Execute request to get album tracks
                Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                TrackSimplified[] items = trackSimplifiedPaging.getItems();
                // Put name of track and corresponding uri into map
                for (TrackSimplified item : items) {
                    releasesMap.put(item.getName(), item.getUri());
                }
            }
            // Duplicates removed
            for (Map.Entry<String, String> entry : releasesMap.entrySet()) {
                releases.add(entry.getValue());
            }
            // return the list
            System.out.println("Ending tracks");
            return releases;
        } catch (ParseException | SpotifyWebApiException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create playlist for designated user with designated name
     *
     * @param userId id of user to create playlist for
     * @param name   name of playlist
     * @return newly created playlistid
     */
    public String createPlaylist(String userId, String name) {
        // OAuth token necessary to create playlist for user
        this.spotifyApi = setOAuthAccessToken();
        // Build request for creating playlist
        CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId, name).build();
        try {
            // Execute request for creating playlist
            Playlist playlist = createPlaylistRequest.execute();
            System.out.println("Playlist succesfully created: " + playlist.getName());
            System.out.println("Playlist id:" + playlist.getId());
            return playlist.getId();
        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add songs to playlist in batches of 90 songs (Spotify limits # of songs added to playlist in request)
     *
     * @param playlistId        id of playlist to add songs
     * @param uris              arraylist of song uris that are added to playlist
     * @param inReplyToStatusId status to reply to
     * @param twitter           twitter object
     */
    public void addTracksToPlaylist(Twitter twitter, long inReplyToStatusId, String playlistId, ArrayList<String> uris) {
        String[] uriArray = uris.toArray(new String[0]);
        try {
            // OAuth token is needed to add songs to user's playlist
            this.spotifyApi = setOAuthAccessToken();
            int x = 0;
            /* When are there are more than 90 songs to be added, there must be another request to handle each set of 90
             * songs
             */
            while (x < uriArray.length) {
                int y = x + 90;
                if (y > uriArray.length)
                    y = uriArray.length;
                // Range can be 0-90, 91-180, etc.
                String[] uriList = Arrays.copyOfRange(uriArray, x, y);
                // Build add items to playlist request
                AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi.addItemsToPlaylist(playlistId, uriList).build();
                // Execute request to add items to playlist
                final SnapshotResult snapshotResult = addItemsToPlaylistRequest.execute();
                System.out.println("Snapshot ID: " + snapshotResult.getSnapshotId());
                x += 90;
            }
            twitter.replyTweet(inReplyToStatusId, "Poggers. This tweet was automated. Playlist is here at https://open.spotify.com/playlist/" + playlistId);
        } catch (SpotifyWebApiException | ParseException | IOException e) {
            e.printStackTrace();
            twitter.replyTweet(inReplyToStatusId, "Unable to add songs to playlist. Please try again later.");
        }
    }

    /**
     * Sets the access token via a client credentials request (token and key generated for Spotify Developer Project)
     *
     * @return spotifyAPI object
     */
    public SpotifyApi setToken() {
        try {
            ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return spotifyApi;
    }

    /**
     * Refreshes the access token when it expires
     *
     * @return spotifyAPI object
     */
    public SpotifyApi setOAuthAccessToken() {
        try {
            // Builds the refresh request
            final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                    .build();
            // Executes the refresh request
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest
                    .execute();
            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return spotifyApi;
    }
}