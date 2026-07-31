package config;

import java.io.File;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigLoader {

	private static Config config;
	private static ObjectMapper objectMapper = new ObjectMapper();

	public static void init() {
		try {
			config = objectMapper.readValue(new File("config.json"), Config.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static BaseConfig base() {
		return config.base();
	}
	
	public static MeetupConfig meetup() {
		return config.meetup();
	}
	
	public static SpotifyConfig spotify() {
		return config.spotify();
	}

	public static long getSnowflake(String key) {
		return config.getSnowflake(key);
	}
	
	private static class Config {

		@JsonProperty("base")
		private BaseConfig baseConfig;
		@JsonProperty("meetup")
		private MeetupConfig meetupConfig;
		@JsonProperty("spotify")
		private SpotifyConfig spotifyConfig;
		@JsonProperty("snowflakes")
		private HashMap<String, Long> discordSnowflakes;
		
		public BaseConfig base() {
			return baseConfig;
		}

		public MeetupConfig meetup() {
			return meetupConfig;
		}

		public SpotifyConfig spotify() {
			return spotifyConfig;
		}

		public long getSnowflake(String key) {
			return discordSnowflakes.getOrDefault(key, 0L).longValue();
		}

	}

}
