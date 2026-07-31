package config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetupConfig {

	@JsonProperty("clientId")
	private String clientId;
	@JsonProperty("clientSecret")
	private String clientSecret;
	@JsonProperty("refreshToken")
	private String refreshToken;

	public String getClientId() {
		return clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

}
