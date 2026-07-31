package gamebot;

import config.ConfigLoader;
import discord4j.common.util.Snowflake;

public enum EvgIds {	
	SERVER("server"),
	
	INTRODUCTIONS_CHANNEL("ch_intro"),
	CONSOLE_CHANNEL("ch_console"),
	LOG_CHANNEL("ch_log"),
	GENERAL_CHANNEL("ch_general"),
	MUSIC_CHANNEL("ch_music"),
	MEETUP_CHANNEL("ch_meetup"),
	ADMIN_MODS_CHANNEL("ch_admin"),
	
	ADMIN_ROLE("r_admin"),
	MODERATOR_ROLE("r_mod"),
	VERIFIED_ROLE("r_verified"),
	MEETUP_ROLE("r_meetupver"),
	NEWACCOUNT_ROLE("r_newacc"),
	
	BOT_MEMBER("m_bot"),
	PETE_MEMBER("m_pete"),
	ADELE_MEMBER("m_adele"),
	DANIEL_MEMBER("m_daniel"),
	
	GENERAL_CATEGORY("cat_general"),
	TOPICS_CATEGORY("cat_topics"),
	GENRES_CATEGORY("cat_genres");
	
	private long id;
	
	EvgIds(String key) {
		this.id = ConfigLoader.getSnowflake(key);
	}
	
	public long id() {
		return id;
	}
	
	public Snowflake snow() {
		return Snowflake.of(id);
	}
	
}
