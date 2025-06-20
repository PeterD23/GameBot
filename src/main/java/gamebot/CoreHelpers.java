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
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

public class CoreHelpers {

	protected long CONSOLE = 731604070573408348L;
	protected long BOT_ID = 731598251437981717L;
	protected long LOG = 902582146437349456L;

	protected long GENERAL = 731597823640076322L;
	protected long MUSIC = 797063557341773834L;

	protected long ADMIN_ROLE = 731604497435983992L;

	private GatewayDiscordClient cli;
	private Guild guild;

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

	protected static String getEveryoneMention() {
		return "@everyone";
	}
	
	protected static String getUserMention(long id) {
		return "<@"+id+">";
	}
	
	protected Guild getGuild() {
		return guild;
	}

	protected Mono<String> mentionUsersWithRole(long roleId) {
		return getGuild()
				.getMembers()
				.filter(usr -> usr.getRoleIds().contains(Snowflake.of(roleId)))
				.map(member -> member.getMention())
				.collectList()
				.map(list -> String.join("\n", list));
	}
	
	protected boolean isAdmin(Member usr) {
		if (Utils.adminsDenied())
			return false;
		return usr.getRoleIds().stream().anyMatch(p -> p.asLong() == ADMIN_ROLE);
	}

	protected Mono<Member> getUserById(long id) {
		return getGuild().getMemberById(Snowflake.of(id));
	}

	protected Mono<Void> editMessage(long channelId, long messageId, String newMessage) {
		ChannelLogger.logMessageInfo("Editing message ID " + messageId + " with String of length " + newMessage.length());
		return getMessage(channelId, messageId).flatMap(message -> message.edit().withContentOrNull(newMessage)).then();
	}

	protected Mono<Void> deleteMessage(long channelId, long messageId, String reason) {
		ChannelLogger.logMessageInfo("Deleting message ID " + messageId + " with reason "+reason);
		return getMessage(channelId, messageId).flatMap(message -> message.delete(reason)).then();
	}

	protected Mono<Message> sendMessage(long channelId, String content) {
		ChannelLogger.logMessageInfo("Creating message to send to Channel " + channelId + " with content '"+content+"'");
		return getChannel(channelId).flatMap(channel -> channel.createMessage(content));
	}
	
	protected Mono<Void> sendReply(Message message, String content) {
		return getChannel(message.getChannelId().asLong()).flatMap(channel ->
				channel.createMessage(content).withMessageReferenceId(message.getId())).then();
	}
	
	protected Mono<Message> sendMessage(long channelId, ArrayList<TopLevelMessageComponent> components) {
		return getChannel(channelId).flatMap(channel -> channel.createMessage().withComponents(components));
	}
	
	protected Mono<Void> editMessage(long channelId, long messageId, ArrayList<TopLevelMessageComponent> components) {
		int length = components.stream().mapToInt(component -> Utils.recursiveLength(component.getData())).sum();
		ChannelLogger.logMessageInfo("Editing message ID " + messageId + " with String of length " + length);
		return getMessage(channelId, messageId).flatMap(message -> message.edit().withComponentsOrNull(components).then());
	}

	protected Mono<Message> getMessage(long channelId, long messageId) {
		return getChannel(channelId).flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)));
	}
	
	protected Mono<TextChannel> getChannel(long id) {
		return getGuild().getChannelById(Snowflake.of(id)).ofType(TextChannel.class);
	}

	protected static boolean hasRole(Member member, long roleId) {
		return member.getRoleIds().contains(Snowflake.of(roleId));
	}

}
