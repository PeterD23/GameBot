package gamebot;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class ChannelLogger {

	// Change ME to the user that should be pinged if something bad happens
	private static long ME = 97036843924598784L;
	private static Member userToPing;

	private static TextChannel logChannel;
	public static long LOG = 902582146437349456L;
	private static Logger log = Loggers.getLogger("logger");
	private static int maxStackLength = 8;

	public static Mono<Void> init(Guild guild) {
		return guild.getChannelById(Snowflake.of(LOG)).ofType(TextChannel.class).flatMap(channel -> {
			logChannel = channel;
			return guild.getMembers()
					.filter(p -> p.getId().asLong() == ME)
					.next()
					.flatMap(member -> Mono.fromRunnable(() -> userToPing = member))
					.then(logMessage(":white_check_mark:", ":sparkles: Game Bot v1.1: 'Interaction Update' Successfully online! :sparkles:")); 
		});	
	}

	private static Mono<Void> logMessage(String logEmoji, String message) {
		// Prevents breaking the bot if the reference didn't init properly
		if (logChannel != null)
			return logChannel.createMessage(logEmoji +" - "+message).then();
		return Mono.empty();
	}
	
	public static Mono<Void> logMessageInfo(String message) {
		log.info(message);
		return logMessage(":information_source:", message);
	}
	
	public static Mono<Void> logMessageWarning(String message) {
		log.warn(message);
		return logMessage(":warning:",message);
	}
	
	public static Mono<Void> logMessageError(String prefix, Throwable error) {
		String message = prefix + formatErrorMessage(error);
		log.error(message);
		return logMessage(":no_entry:",message);
	}
	
	public static Mono<Void> logHighPriorityMessage(String prefix, Throwable error) {
		String message = prefix + formatErrorMessage(error);
		log.error(message);
		// Prevents breaking the bot if the reference didn't init properly
		if (logChannel != null)
			return logChannel.createMessage(userToPing.getMention() + " there is a critical issue.\n" + message).then();
		return Mono.empty();
	}

	private static String formatErrorMessage(Throwable throwable) {
		if(throwable == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Type: ").append(throwable.getClass().getName()).append("\n");
		sb.append("Message: ").append(throwable.getMessage()).append("\n");

		StackTraceElement[] stackTrace = throwable.getStackTrace();
		int stackLength = Math.min(maxStackLength, stackTrace.length);
		for(int i = 0; i < stackLength; i++) {
			StackTraceElement element = stackTrace[i];
			String info = element.getFileName() != null ? (element.getFileName() + "." + element.getMethodName() + ":" + element.getLineNumber()) : element.getMethodName();
			sb.append("Stack ["+i+"]:").append(info).append("\n");
		}
		return sb.toString();
	}

}
