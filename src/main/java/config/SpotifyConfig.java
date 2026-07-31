package config;

import com.fasterxml.jackson.annotation.JsonGetter;

public class SpotifyConfig {

	private String clientId;
	private String clientSecret;
	
	@JsonGetter("clientId")
	public String getClientId() {
		return clientId;
	}
	
	@JsonGetter("clientSecret")
	public String getClientSecret() {
		return clientSecret;
	}
	
}
