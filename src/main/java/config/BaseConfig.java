package config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseConfig {

	@JsonProperty("redisHost")
	private String redisHost;
	@JsonProperty("redisPass")
	private String redisPass;
	@JsonProperty("redisStorageKey")
	private String redisStorageKey;
	@JsonProperty("discordAuthKey")
	private String discordAuthKey;

	public String getRedisHost() {
		return redisHost;
	}

	public String getRedisPass() {
		return redisPass;
	}

	public String getRedisStorageKey() {
		return redisStorageKey;
	}

	public String getDiscordKey() {
		return discordAuthKey;
	}

}
