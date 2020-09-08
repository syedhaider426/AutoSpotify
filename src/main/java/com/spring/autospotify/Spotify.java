package com.spring.autospotify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.special.SnapshotResult;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.albums.GetAlbumsTracksRequest;
import com.wrapper.spotify.requests.data.artists.GetArtistsAlbumsRequest;
import com.wrapper.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.CreatePlaylistRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchArtistsRequest;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;


public class Spotify {
    private SpotifyApi spotifyApi;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    String spotifyClientId = prop.getProperty("spotifyClientId");
    String spotifyClientSecret = prop.getProperty("spotifyClientSecret");

    JDBCUtil db = JDBCUtil.getInstance();


    public Spotify() throws IOException {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(spotifyClientId)
                .setClientSecret(spotifyClientSecret)
                .build();
        this.spotifyApi = spotifyApi;
    }

    public SpotifyApi setToken() throws ParseException, SpotifyWebApiException, IOException {
        if (spotifyApi.getAccessToken() == null) {
            ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            System.out.println("Token set");
        }
        return spotifyApi;
    }

    // Search artist will do an api call, verify the artist exists in Spotify, and return spotifyid back
    public ArrayList<String> searchArtist(Map<String, String> artistMap) throws ParseException, SpotifyWebApiException, IOException, SQLException, ClassNotFoundException {
        ArrayList<String> artists = new ArrayList<>();
        for (Map.Entry<String, String> entry : artistMap.entrySet()) {
            String originalArtist = entry.getKey();
            String parsedArtist = entry.getValue();
            String spotifyId;
            SearchArtistsRequest searchArtistsRequest;
            this.spotifyApi = setToken();
            if (parsedArtist.contains(" X ")) {
                spotifyId = db.getSpotifyID(originalArtist);
                searchArtistsRequest = spotifyApi.searchArtists(originalArtist).limit(10).build();
            } else {
                spotifyId = db.getSpotifyID(parsedArtist);
                searchArtistsRequest = spotifyApi.searchArtists(parsedArtist).limit(10).build();
            }
            if (spotifyId.length() > 0) {
                System.out.println("Found artists in database: " + originalArtist);
                artists.add(spotifyId);
                continue;
            }
            try {
                final Paging<Artist> artistPaging = searchArtistsRequest.execute();
                Artist[] artistArr = artistPaging.getItems();
                if (artistArr.length == 0) {
                    System.out.println("No artist found with name: " + parsedArtist);
                    continue;
                }
                for (int x = 0; x < artistArr.length; x++) {
                    String spotifyName = artistArr[x].getName().toUpperCase();
                    String id = artistArr[x].getId();
                    LevenshteinDistance l = new LevenshteinDistance();
                    int originalLeven = l.apply(originalArtist, spotifyName);
                    int parsedLeven = l.apply(parsedArtist, spotifyName);
                    if (originalArtist.equals(spotifyName) || parsedArtist.equals(spotifyName) || originalLeven <= 2 || parsedLeven <= 2) {
                        if ((originalLeven > 0 && originalLeven <= 2) || (parsedLeven > 0 && parsedLeven <= 2)) {
                            if (parsedArtist.contains(" X "))
                                db.insertArtist(originalArtist, id);
                            else
                                db.insertArtist(parsedArtist, id);
                        }
                        db.insertArtist(spotifyName, id);
                        System.out.println("We found the artist in the api: " + spotifyName);
                        artists.add(id);
                        break;
                    }
                }
                System.out.println("No artist found with name: " + originalArtist + " or " + parsedArtist);
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }
        }
        return artists;
    }

    // Get the releases based off the date
    public ArrayList<String> getReleases(ArrayList<String> artistIdList, LocalDateTime tweetDate) throws ParseException, SpotifyWebApiException, IOException {
        System.out.println("Loading releases");
        this.spotifyApi = setToken();
        ArrayList<String> spotifyIdList = new ArrayList<>();
        int counter = 0;
        Boolean found = false;
        for (int x = 0; x < artistIdList.size(); x++) {
            if (counter > 0)
                x--;
            GetArtistsAlbumsRequest getArtistsAlbumsRequest = spotifyApi.getArtistsAlbums(artistIdList.get(x))
                    .limit(50)
                    .offset(counter)
                    .build();
            try {
                final Paging<AlbumSimplified> albums = getArtistsAlbumsRequest.execute();
                AlbumSimplified[] items = albums.getItems();
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (int y = 0; y < items.length; y++) {
                    LocalDateTime releaseDate = LocalDate.parse(items[y].getReleaseDate(), format).atStartOfDay();
                    long d1 = Duration.between(tweetDate, releaseDate).toDays();
                    if (d1 <= 7 && d1 >= -7) {
//                        System.out.println(items[y].getReleaseDate());
//                        System.out.println("We found the " + items[y].getAlbumType() +": " + items[y].getName());
                        spotifyIdList.add(items[y].getId());
                        found = true;
                        counter = 0;
                    }
                }
                if (found == false)
                    counter += 50;
            } catch (IOException | SpotifyWebApiException | ParseException | DateTimeParseException e) {
                if (e instanceof DateTimeParseException)
                    continue;
                else
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
            for (int x = 0; x < albumReleases.size(); x++) {
                GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi.getAlbumsTracks(albumReleases.get(x)).build();
                Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                TrackSimplified[] items = trackSimplifiedPaging.getItems();
                for (int y = 0; y < items.length; y++) {
                    releasesMap.put(items[y].getName(), items[y].getUri());
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

    // Get oauth token
    public SpotifyApi getOAuthAccessToken() {
        spotifyApi.setAccessToken("");
        return spotifyApi;
    }

    // Create playlist
    public String createPlaylist(String userId, String name) {
        this.spotifyApi = getOAuthAccessToken();
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
            this.spotifyApi = getOAuthAccessToken();
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


}
