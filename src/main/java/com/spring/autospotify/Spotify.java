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


public class Spotify {
    private SpotifyApi spotifyApi;
    private JDBCUtil db;

    public Spotify(JDBCUtil db) {
        Properties prop;
        try {
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


    // Search artist will do an api call, verify the artist exists in Spotify, and return spotifyid back
    public ArrayList<String> searchArtist(ArrayList<String> artistList) {
        System.out.println("Searching artists");
        ArrayList<String> artists = new ArrayList<>();
        String spotifyId;
        for (String artist : artistList) {
            spotifyId = db.getSpotifyID(artist);
            if (spotifyId.length() > 0) {
                artists.add(spotifyId);
                continue;
            }
            this.spotifyApi = setToken();
            System.out.println("Checking artist: " + artist);
            SearchArtistsRequest searchArtistsRequest = spotifyApi.searchArtists(artist).limit(10).build();
            try {
                final Paging<Artist> artistPaging = searchArtistsRequest.execute();
                Artist[] artistArr = artistPaging.getItems();
                if (artistArr.length == 0) {
                    System.out.println("No artist found with name: " + artist);
                    continue;
                }
                for (Artist value : artistArr) {
                    String spotifyName = value.getName().toUpperCase();
                    String id = value.getId();
                    LevenshteinDistance l = new LevenshteinDistance();
                    int originalLeven = l.apply(artist, spotifyName);
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

    // Get the releases based off the date
    public ArrayList<String> getReleases(ArrayList<String> artistIdList, LocalDateTime tweetDate) {
        System.out.println("Loading releases");
        ArrayList<String> spotifyIdList = new ArrayList<>();
        int counter = 0;
        for (int x = 0; x < artistIdList.size(); x++) {
            boolean found = false;
            this.spotifyApi = setToken();
            System.out.println("Checking artist: " + artistIdList.get(x));
            GetArtistsAlbumsRequest getArtistsAlbumsRequest = spotifyApi.getArtistsAlbums(artistIdList.get(x))
                    .limit(50)
                    .market(CountryCode.US)
                    .offset(counter)
                    .build();
            try {
                final Paging<AlbumSimplified> albums = getArtistsAlbumsRequest.execute();
                AlbumSimplified[] items = albums.getItems();
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (AlbumSimplified item : items) {
                    try {
                        LocalDateTime releaseDate = LocalDate.parse(item.getReleaseDate(), format).atStartOfDay();
                        long d1 = Duration.between(tweetDate, releaseDate).toDays();
                        if (d1 <= 9 && d1 >= -9) {
                            spotifyIdList.add(item.getId());
                            found = true;
                            counter = 0;
                        }
                    } catch (DateTimeParseException ex) {
                        continue;
                    }
                }
                if (found) {
                    System.out.println("Found at least one song!!!!!");
                } else if (!found && counter == 0 || (!found && counter == 50 && x == artistIdList.size() - 1)) {
                    System.out.println("No song found. Checking next 50");
                    x--;    //check the same artist
                    counter += 50;
                } else if (!found && counter == 50 && x != artistIdList.size() - 1) {
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

    // Get the tracks based off the date
    public ArrayList<String> getAlbumTracks(ArrayList<String> albumReleases) {
        System.out.println("Loading tracks");
        ArrayList<String> releases = new ArrayList<>();
        Map<String, String> releasesMap = new HashMap<>();
        try {
            for (String albumRelease : albumReleases) {
                GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi.getAlbumsTracks(albumRelease).build();
                Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                TrackSimplified[] items = trackSimplifiedPaging.getItems();
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


    // Create playlist
    public String createPlaylist(String userId, String name) {
        this.spotifyApi = setOAuthAccessToken();
        CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId, name).build();
        try {
            Playlist playlist = createPlaylistRequest.execute();
            System.out.println("Playlist succesfully created: " + playlist.getName());
            System.out.println("Playlist id:" + playlist.getId());
            return playlist.getId();
        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Add songs to playlist
    public Boolean addSongsToPlaylist(String playlistId, ArrayList<String> uris) {
        String[] uriArray = uris.toArray(new String[0]);
        try {
            this.spotifyApi = setOAuthAccessToken();
            int x = 0;
            while (x < uriArray.length) {
                int y = x + 90;
                if (y > uriArray.length)
                    y = uriArray.length;
                String[] uriList = Arrays.copyOfRange(uriArray, x, y);
                AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi.addItemsToPlaylist(playlistId, uriList).build();
                final SnapshotResult snapshotResult = addItemsToPlaylistRequest.execute();
                System.out.println("Snapshot ID: " + snapshotResult.getSnapshotId());
                x += 90;
                //}
            }
            return true;
        } catch (SpotifyWebApiException | ParseException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }


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

    public SpotifyApi setOAuthAccessToken() {
        try {
            final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                    .build();
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return spotifyApi;
    }


}
