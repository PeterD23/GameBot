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
	public static long LOG = 902582146437349456L;
	private static Logger log = Loggers.getLogger("logger");

	public static void init(Guild guild) {
		logChannel = (TextChannel) guild.getChannelById(Snowflake.of(LOG)).block();
		userToPing = guild.getMembers().filter(p -> p.getId().asLong() == ME).next().block();
	}

	public static void logMessage(String message) {
		log.info(message);
		// Prevents breaking the bot if the reference didn't init properly
		if (logChannel != null)
			logChannel.createMessage(message).block();
	}
	
	public static void logWithoutMessage(String message) {
		log.info(message);
	}

	public static void logHighPriorityMessage(String prefix, Throwable error) {
		String message = prefix + formatErrorMessage(error);
		log.error(message);
		// Prevents breaking the bot if the reference didn't init properly
		if (logChannel != null)
			logChannel.createMessage(userToPing.getMention() + " there is a critical issue.\n" + message).block();
	}

	public static String mentionCreator() {
		return userToPing.getMention();
	}

	private static String formatErrorMessage(Throwable throwable) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Type: ").append(throwable.getClass().getName()).append("\n");
		sb.append("Message: ").append(throwable.getMessage()).append("\n");

		StackTraceElement[] stackTrace = throwable.getStackTrace();
		if (stackTrace.length > 0) {
			StackTraceElement element = stackTrace[0];
			sb.append("File: ").append(element.getFileName()).append("\n");
			sb.append("Line: ").append(element.getLineNumber()).append("\n");
			sb.append("Method: ").append(element.getMethodName()).append("\n");
		}
		return sb.toString();
	}

}
