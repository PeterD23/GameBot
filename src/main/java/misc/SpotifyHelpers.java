package misc;

import java.util.Random;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsItemsRequest;

import config.ConfigLoader;
import config.SpotifyConfig;
import gamebot.CoreHelpers;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class SpotifyHelpers extends CoreHelpers {

	private static Logger log = Loggers.getLogger("spotify");
	private static SpotifyApi api;
	private static Random random = new Random();

	public static Mono<Void> init() {
		return Mono.fromRunnable(() -> {
			SpotifyConfig config = ConfigLoader.spotify();
			String clientId = config.getClientId();
			String clientSecret = config.getClientSecret();
			if (clientId == null || clientSecret == null) {
				log.error("Client ID and/or Client Secret not specified.");
				return;
			}
			api = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret).build();
		});
	}

	private static void getAccessToken() {
		/* Create a request object. */
		ClientCredentialsRequest request = api.clientCredentials().build();

		try {
			ClientCredentials clientCredentials = request.execute();
			api.setAccessToken(clientCredentials.getAccessToken());

			log.info("Successfully retrieved an access token! " + clientCredentials.getAccessToken());
			log.info("The access token expires in " + clientCredentials.getExpiresIn() + " seconds");
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Access token retrieval failed. 123-PLSFIX-ACCESS");
		}
	}

	public static String recommendSong(String playlistId) {
		if (api == null) {
			log.error("Unable to recommend song due to uninitialised API.");
			return "";
		}
		getAccessToken();
		GetPlaylistsItemsRequest itemsRequest = api.getPlaylistsItems(playlistId).build();
		try {
			Paging<PlaylistTrack> tracks = itemsRequest.execute();
			Integer song = new Integer(random.nextInt(tracks.getTotal().intValue()));

			itemsRequest = api.getPlaylistsItems(playlistId).offset(song).build();
			PlaylistTrack track = itemsRequest.execute().getItems()[0];
			return track.getTrack().getExternalUrls().get("spotify");
		} catch (Exception e) {
			log.error("429, try again later");
			return "";
		}
	}

}
