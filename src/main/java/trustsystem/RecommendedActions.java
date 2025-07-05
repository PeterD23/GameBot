package trustsystem;

import static trustsystem.RecommendedActions.RecommendedAction.CANCEL_REPORT;
import static trustsystem.RecommendedActions.RecommendedAction.DELETE_MESSAGES;
import static trustsystem.RecommendedActions.RecommendedAction.DELETE_MESSAGES_AND_TIMEOUT;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.Separator.SpacingSize;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.GuildMemberEditSpec;
import discord4j.rest.util.Color;
import gamebot.ChannelLogger;
import gamebot.EvgIds;
import gamebot.GameBot;
import misc.RedisConnector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Loggers;
import trustsystem.TrustSystem.TrustRating;

public class RecommendedActions {

	private static String key = "gamebot:ReportedUsers";
	private static GatewayDiscordClient client = GameBot.gateway;
	private static Snowflake guildId = Snowflake.of(GameBot.SERVER);

	public interface IRecommendedAction {
		Mono<?> perform(ReportedUser report, TrustRating confidence);
	}

	public enum RecommendedAction {

		DELETE_MESSAGES((r, c) -> deleteMessageAndFlag(r)),
		DELETE_MESSAGES_AND_TIMEOUT((r, c) -> deleteMessageAndTimeout(r)), IGNORE((r, c) -> ignore(r)),
		CANCEL_REPORT((r, c) -> cancelReport(r)), ADD_CONFIDENCE((r, c) -> addConfidence(r, c));

		private IRecommendedAction action;

		RecommendedAction(IRecommendedAction action) {
			this.action = action;
		}

		public Mono<?> go(ReportedUser report, TrustRating confidence) {
			return action.perform(report, confidence);
		}
	}

	public static Mono<Void> invokeAdminOption(String[] id) {
		String action = id[0];
		String userId = id[1];
		
		return RedisConnector.readValue(key, userId, ReportedUser.class).flatMap(report -> {
			switch(action) {
			case "ts.timeout":
				return DELETE_MESSAGES_AND_TIMEOUT.go(report.get(), null).then();
			case "ts.delete":
				return DELETE_MESSAGES.go(report.get(), null).then();
			case "ts.cancel":
				return CANCEL_REPORT.go(report.get(), null).then();
			}
			
			return Mono.empty();
		});
		
	}
	
	private static Mono<Integer> deleteMessageAndFlag(ReportedUser report) {
		return Flux.fromIterable(report.reportedMessages)
				.log(Loggers.getLogger("MessageToDeleteFlux"))
				.flatMap(reportedMessage -> client.getGuildById(guildId)
						.flatMap(guild -> guild.getChannelById(reportedMessage.getChannel()))
						.ofType(TextChannel.class)
						.flatMap(channel -> channel.getMessageById(reportedMessage.getId())
								.onErrorResume(t -> Mono.empty())) // If message was deleted beforehand, discard it from the report
						.flatMap(message -> message.delete(String.join(",", reportedMessage.reasons())).then())
						.then(ChannelLogger.logMessageInfo("Message "+reportedMessage.messageId+ " was deleted for reasons "+String.join(",", reportedMessage.reasons()))))
				.count()
				.flatMap(count -> createUpdateReportMessage(report.clear(), "Deleted "+count+" messages."))
				.then(Mono.just(10));
	}

	private static Mono<Integer> deleteMessageAndTimeout(ReportedUser report) {
		String action = "Deleted "+report.reportedMessages.size()+ " messages and timed out user.";
		return Flux.fromIterable(report.reportedMessages)
				.flatMap(reportedMessage -> client.getGuildById(guildId)
						.flatMap(guild -> guild.getChannelById(reportedMessage.getChannel())
							.ofType(TextChannel.class)
							.flatMap(channel -> channel.getMessageById(reportedMessage.getId()))
							.flatMap(message -> message.delete(String.join(",", reportedMessage.reasons()))
								.onErrorResume(t -> Mono.empty())) // If message was deleted beforehand, discard it from the report
							.then(guild.getMemberById(Snowflake.of(report.getReported())))
							.flatMap(member -> member.edit(GuildMemberEditSpec
									.builder()
									.communicationDisabledUntilOrNull(Instant.now().plus(Duration.ofDays(3)))
									.build())
									)
								)
						)
				.then(createUpdateReportMessage(report.clear(), action))
				.then(Mono.just(20));
	}

	private static Mono<Void> addCross(ReportedUser report){
		return Flux.fromIterable(report.reportedMessages)
		.flatMap(reportedMessage -> client.getGuildById(guildId)
				.flatMap(guild -> guild.getChannelById(reportedMessage.getChannel()))
				.ofType(TextChannel.class)
				.flatMap(channel -> channel.getMessageById(reportedMessage.getId())
						.onErrorResume(t -> Mono.empty())) // If message was deleted beforehand, discard it from the report
				.flatMap(message -> message.addReaction(ReactionEmoji.codepoints("U+274C"))))
		.then();
	}
	
	private static Mono<Boolean> addConfidence(ReportedUser report, TrustRating confidence) {
		return RedisConnector.readValue(key, report.getReported(), ReportedUser.class)
				.flatMap(optional -> {
					if (optional.isPresent()) {
						boolean overThreshold = report.addConfidence(confidence);
						return RedisConnector.mergeObject(key, report.getReported(), ReportedUser.class, report)
								.then(addCross(report))
								.then(createUpdateReportMessage(report, null))
								.then(Mono.just(overThreshold));
					}
					return Mono.empty();
				});
	}
	
	private static Mono<Void> cancelReport(ReportedUser report){
		return RedisConnector.deleteEntry(key, report.reportedUserId)
				.then(client.getGuildById(guildId)
						.flatMap(guild -> guild.getChannelById(EvgIds.ADMIN_MODS_CHANNEL.snow()))
						.ofType(TextChannel.class)
						.flatMap(channel -> channel.getMessageById(Snowflake.of(report.reportId)))
						.flatMap(message -> message.delete()))
				.then();
	}
	
	private static Mono<Void> createUpdateReportMessage(ReportedUser report, String action){
		String reportId = report.reportId;
		return client.getGuildById(guildId)
				.flatMap(guild -> guild.getChannelById(EvgIds.ADMIN_MODS_CHANNEL.snow()))
				.ofType(TextChannel.class)
				.flatMap(channel -> reportId != null
					? channel.getMessageById(Snowflake.of(reportId))
						.flatMap(message -> message.edit().withComponents(constructMessage(report, action)))
						.then()
					: channel.createMessage()
						.withComponents(constructMessage(report, action))
						.flatMap(newMessage -> Mono.fromRunnable(() -> report.reportId = newMessage.getId().asString())))
				.then(RedisConnector.mergeObject(key, report.getReported(), ReportedUser.class, report))
				.then();
	}
	
	private static TopLevelMessageComponent constructMessage(ReportedUser report, String action) {
		List<String> reporterMentions = report.reportedBy.stream().map(reporter -> "<@"+reporter+">").collect(Collectors.toList());
		List<String> reportedMessages = report.reportedMessages.stream().map(message -> message.contentOrImages()+" for reasons `"+String.join(",", message.reasons)+"`").collect(Collectors.toList());
		String idString = report.reportedUserId;
		ActionRow buttons = ActionRow.of(Button.danger("ts.timeout_"+idString, "Timeout"), Button.primary("ts.delete_"+idString, "Delete"), Button.success("ts.cancel_"+idString, "Cancel Report"));
		return Container.of(Color.RED, 
				TextDisplay.of("Report against <@"+report.getReported()+"> with Confidence: "+report.confidence), 
				Separator.of(),
				TextDisplay.of("Reported Messages: \n"+String.join("\n", reportedMessages)),
				Separator.of(SpacingSize.LARGE),
				TextDisplay.of("Reported by: "+String.join(",", reporterMentions)),
				action != null ? TextDisplay.of("# Action Taken: "+action) : buttons);
	}

	private static Mono<Integer> ignore(ReportedUser report) {
		return createUpdateReportMessage(report, null).then(Mono.just(0));
	}

}
