package com.spring.autospotify;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchArtistsRequest;
import org.apache.hc.core5.http.ParseException;


import java.io.IOException;
import java.util.Properties;


public class Spotify {
    private SpotifyApi spotifyApi;
    GetPropertyValues properties = new GetPropertyValues();
    Properties prop = properties.getPropValues();
    String spotifyClientId = prop.getProperty("spotifyClientId");
    String spotifyClientSecret = prop.getProperty("spotifyClientSecret");

    public Spotify() throws IOException {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(spotifyClientId)
                .setClientSecret(spotifyClientSecret)
                .build();
        this.spotifyApi = spotifyApi;
    }

    public SpotifyApi setToken() throws ParseException, SpotifyWebApiException, IOException {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        ClientCredentials clientCredentials = clientCredentialsRequest.execute();
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
        return spotifyApi;
    }

    // Search artist will do an api call, verify the artist exists in Spotify, and return spotifyid back
    public String searchArtist(String originalArtist, String parsedArtist) throws ParseException, SpotifyWebApiException, IOException {
        this.spotifyApi = setToken();
        SearchArtistsRequest searchArtistsRequest = spotifyApi.searchArtists(parsedArtist).limit(50).build();
        try{
            final Paging<Artist> artistPaging = searchArtistsRequest.execute();
            int total = artistPaging.getTotal();
            if(total == 0){
                System.out.println("No artist found");
                return "";
            }
            Artist[] artists = artistPaging.getItems();
            String oArtist = originalArtist.toUpperCase();
            String pArtist = parsedArtist.toUpperCase();
            for(int x = 0; x < total; x++){
                String spotifyName = artists[x].getName().toUpperCase();
                if(oArtist.equals(spotifyName) || pArtist.equals(spotifyName)){
                    System.out.println("We found the artist: " + spotifyName);
                    return artists[x].getId();
                }
            }
        }
        catch (IOException| SpotifyWebApiException| ParseException e){
            e.printStackTrace();
        }
        return null;
    }




}
