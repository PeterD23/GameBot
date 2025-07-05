package gamebot;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import misc.Utils;
import reactor.core.publisher.Mono;

public class CoreHelpers {

	private GatewayDiscordClient cli;
	protected Guild guild;

	protected PermissionSet readSend = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES,
			Permission.READ_MESSAGE_HISTORY);

	protected Mono<Void> init(GuildCreateEvent event) {
		guild = event.getGuild();
		cli = event.getClient();
		return cli.edit().withUsername("Game Bot").then();
	}
	
	protected static String mentionMe() {
		return "<@97036843924598784>";
	}
	
	protected static String getUserMention(String id) {
		return "<@"+id+">";
	}

	protected Mono<String> mentionUsersWithRole(long roleId) {
		return guild
				.getMembers(EntityRetrievalStrategy.STORE_FALLBACK_REST)
				.filter(usr -> usr.getRoleIds().contains(Snowflake.of(roleId)))
				.map(member -> member.getMention())
				.collectList()
				.map(list -> String.join("\n", list));
	}
	
	protected static boolean isAdmin(Member usr) {
		if (Utils.adminsDenied())
			return false;
		return usr.getRoleIds().stream().anyMatch(p -> p.asLong() == EvgIds.ADMIN_ROLE.id());
	}

	protected Mono<Member> getUserById(long id) {
		return guild.getMemberById(Snowflake.of(id), EntityRetrievalStrategy.STORE_FALLBACK_REST);
	}

	protected Mono<Void> deleteMessage(long channelId, long messageId, String reason) {
		return ChannelLogger.logMessageInfo("Deleting message ID " + messageId + " with reason "+reason)
				.then(getMessage(channelId, messageId)
						.flatMap(message -> message.delete(reason)))
				.then()
				.onErrorResume(t -> ChannelLogger.logMessageError("Error in Deleting Message "+messageId, t)); // Don't kill the whole Interval Listener, just fail the message
	}

	protected Mono<Message> sendMessage(long channelId, String content) {
		return ChannelLogger.logMessageInfo("Creating message to send to Channel " + channelId + " with content '"+content+"'")
				.then(getChannel(channelId)
						.flatMap(channel -> channel.createMessage(content))
						);
	}
	
	protected Mono<Void> sendReply(Message message, String content) {
		return getChannel(message.getChannelId().asLong()).flatMap(channel ->
				channel.createMessage(content).withMessageReferenceId(message.getId())).then();
	}
	
	protected Mono<Message> sendMessage(long channelId, ArrayList<TopLevelMessageComponent> components) {
		return ChannelLogger.logMessageInfo("Creating message to send to Channel " + channelId + " with component size '"+components.size()+"'")
				.then(getChannel(channelId)
						.flatMap(channel -> channel.createMessage()
								.withComponents(components)
								)
						);
	}
	
	protected Mono<Void> editMessage(long channelId, long messageId, ArrayList<TopLevelMessageComponent> components) {
		int length = components.stream().mapToInt(component -> Utils.recursiveLength(component.getData())).sum();
		return ChannelLogger.logMessageInfo("Editing message ID " + messageId + " with String of length " + length)
				.then(getMessage(channelId, messageId)
						.flatMap(message -> message.edit().withContentOrNull(null)
								.withComponentsOrNull(components)
						))
				.then()
				.onErrorResume(t -> ChannelLogger.logMessageError("Error in Editing Message "+messageId, t)); // Don't kill the whole Interval Listener, just fail the message
	}

	protected Mono<Message> getMessage(long channelId, long messageId) {
		return getChannel(channelId).flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)));
	}
	
	protected Mono<TextChannel> getChannel(long id) {
		return guild.getChannelById(Snowflake.of(id), EntityRetrievalStrategy.STORE_FALLBACK_REST).ofType(TextChannel.class);
	}

	protected static boolean hasRole(Member member, long roleId) {
		return member.getRoleIds().contains(Snowflake.of(roleId));
	}

}
