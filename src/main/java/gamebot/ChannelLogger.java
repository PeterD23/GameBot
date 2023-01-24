package gamebot;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.util.Logger;
import reactor.util.Loggers;

public class ChannelLogger {

	// Change ME to the user that should be pinged if something bad happens
	private static long ME = 97036843924598784L;
	private static Member userToPing;
	
	private static TextChannel logChannel;
	private static long LOG = 902582146437349456L;
	private static Logger log = Loggers.getLogger("logger");
	
	public static void init(Guild guild) {
		logChannel = (TextChannel) guild.getChannelById(Snowflake.of(LOG)).block();
		userToPing = guild.getMembers().filter(p -> p.getId().asLong() == ME).next().block();
	}
	
	public static void logMessage(String message) {
		log.info(message);
		logChannel.createMessage(message).block();
	}
	
	public static void logHighPriorityMessage(String message) {
		log.error(message);
		logChannel.createMessage(userToPing.getMention() + " there is a critical issue.\n"+message).block();
	}
	
	public static String mentionCreator() {
		return userToPing.getMention();
	}
	
}
