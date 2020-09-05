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
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;


public class Spotify {
    private SpotifyApi spotifyApi;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    String spotifyClientId = prop.getProperty("spotifyClientId");
    String spotifyClientSecret = prop.getProperty("spotifyClientSecret");

    JDBCUtil db = new JDBCUtil();


    public Spotify() throws IOException, SQLException, ClassNotFoundException {

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
    public ArrayList<String> searchArtist(Map<String,String> artistMap) throws ParseException, SpotifyWebApiException, IOException, SQLException {
        ArrayList<String> artists = new ArrayList<>();
        for(Map.Entry<String, String> entry:artistMap.entrySet()) {
            String originalArtist = entry.getKey();
            String parsedArtist = entry.getValue();
            String oArtist = originalArtist.toUpperCase();
            String pArtist = parsedArtist.toUpperCase();
            String spotifyId = db.getSpotifyID(pArtist);
            if (spotifyId.length() > 0) {
                System.out.println("We found the artist in the database: " + pArtist);
                artists.add(spotifyId);
                break;
            }
            this.spotifyApi = setToken();
            System.out.println("Checking artist: " + originalArtist);
            SearchArtistsRequest searchArtistsRequest = spotifyApi.searchArtists(pArtist).limit(50).build();
            try {
                final Paging<Artist> artistPaging = searchArtistsRequest.execute();
                Artist[] artistArr = artistPaging.getItems();
                if (artistArr.length == 0) {
                    System.out.println("No artist found with name: " + parsedArtist);
                    break;
                }
                for (int x = 0; x < artistArr.length; x++) {
                    String spotifyName = artistArr[x].getName().toUpperCase();
                    String id = artistArr[x].getId();
                    if (oArtist.equals(spotifyName) || pArtist.equals(spotifyName)) {
                        if (oArtist.equals(spotifyName))
                            db.insertArtist(oArtist, id);
                        else
                            db.insertArtist(pArtist, id);
                        System.out.println("We found the artist in the api: " + spotifyName);
                        artists.add(id);
                        break;
                    }
                }

                System.out.println("No artist found with name: " + parsedArtist);
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }
        }
        return artists;
    }

    public ArrayList<String> getNewReleases(ArrayList<String> artistIdList) throws ParseException, SpotifyWebApiException, IOException {
        this.spotifyApi = setToken();
        ArrayList<String> spotifyIdList = new ArrayList<>();
        for(int x = 0; x < artistIdList.size(); x++) {
            GetArtistsAlbumsRequest getArtistsAlbumsRequest = spotifyApi.getArtistsAlbums(artistIdList.get(x))
                    .build();
            try {
                final Paging<AlbumSimplified> albums = getArtistsAlbumsRequest.execute();
                AlbumSimplified[] items = albums.getItems();
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime current = LocalDate.now().atStartOfDay();
                for (int y = 0; y < items.length; y++) {
                    LocalDateTime releaseDate = LocalDate.parse(items[y].getReleaseDate(), format).atStartOfDay();
                    long d1 = Duration.between(releaseDate, current).toDays();
                    if (d1 <= 28) {
                        System.out.println(items[y].getReleaseDate());
                        System.out.println("We found the " + items[y].getAlbumType() +
                                ": " + items[y].getName());
                        spotifyIdList.add(items[y].getId());
                    }
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }
        }
        return spotifyIdList;
    }

    public ArrayList<String> getAlbumTracks(ArrayList<String> albumReleases){
        ArrayList<String> releases = new ArrayList<>();
        try{
            for(int x = 0; x < albumReleases.size(); x++) {
                GetAlbumsTracksRequest getAlbumsTracksRequest = spotifyApi.getAlbumsTracks(albumReleases.get(x)).build();
                Paging<TrackSimplified> trackSimplifiedPaging = getAlbumsTracksRequest.execute();
                TrackSimplified[] items = trackSimplifiedPaging.getItems();
                for(int y = 0; y < items.length; y++){
                    releases.add(items[y].getUri());
                }
            }
            return releases;
        }
        catch(ParseException | SpotifyWebApiException | IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public SpotifyApi getOAuthAccessToken(){
        spotifyApi.setAccessToken("");
        return spotifyApi;
    }

    public String createPlaylist(String userId, String name)  {
        this.spotifyApi = getOAuthAccessToken();
        CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId,name).build();
        try{
            Playlist playlist = createPlaylistRequest.execute();
            System.out.println("Playlist succesfully created: "  + playlist.getName());
            System.out.println("Playlist id:" + playlist.getId());
            return playlist.getId();
        } catch (ParseException | IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean addSongsToPlaylist(String playlistId, ArrayList<String> uris) {
        this.spotifyApi = getOAuthAccessToken();
        String[] uriArray = uris.toArray(new String[0]);
        try {
            this.spotifyApi = setToken();
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi.addItemsToPlaylist(playlistId,uriArray).build();
            final SnapshotResult snapshotResult = addItemsToPlaylistRequest.execute();
            System.out.println("Snapshot ID: " + snapshotResult.getSnapshotId());
            return true;
        }catch(SpotifyWebApiException | ParseException | IOException e){
            e.printStackTrace();
        }
        return false;
    }




}
