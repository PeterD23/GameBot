package gamebot;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

public class CoreHelpers {

	protected long CONSOLE = 731604070573408348L;
	protected long SERVER = 731597823640076319L;
	protected long BOT_ID = 731598251437981717L;
	protected long LOG = 902582146437349456L;

	protected long GENERAL = 731597823640076322L;
	protected long MUSIC = 797063557341773834L;
	protected long EVENTS = 907696207508406342L;

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

	protected GuildEmoji getEmojiByName(String name) {
		return getGuild().getEmojis().filter(p -> p.getName().equals(name)).next().block();
	}

	protected String mentionUsersWithRole(long roleId) {
		ArrayList<Member> members = new ArrayList<>(getGuild()
				.getMembers()
				.filter(usr -> usr.getRoleIds()
				.contains(Snowflake.of(roleId)))
				.collectList()
				.block());
		String mentions = "";
		for (int i = 0; i < members.size(); i++) {
			mentions += members.get(i).getMention() + "\n";
		}
		return mentions;
	}
	
	protected Mono<Boolean> mentionedBot(String message) {
		return getUserById(BOT_ID).map(bot -> message.contains(bot.getMention()));
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
	
	protected Mono<Message> embedImage(long channelId, String imageName) {
		char ps = File.separatorChar;
		String filePath = System.getProperty("user.home") + ps + "Pictures" + ps + imageName;
		ChannelLogger.logMessageInfo("Looking for " + filePath);
		try (FileInputStream fs = new FileInputStream(filePath)) {
			return getChannel(channelId)
					.flatMap(channel -> { 
						return channel.createMessage(MessageCreateSpec.builder()
								.addFile(imageName, fs)
								.build()
								);
					});
		} catch (Exception e) {
			ChannelLogger.logMessageError("Image acquisition failure: ", e);
			return sendMessage(channelId, "Couldn't find that image, sorry :(");
		}
	}

	protected Mono<Message> getMessage(long channelId, long messageId) {
		return getChannel(channelId).flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)));
	}
	
	protected Mono<TextChannel> getChannel(long id) {
		return getGuild().getChannelById(Snowflake.of(id)).ofType(TextChannel.class);
	}

	protected Mono<Role> createRole(String name) {
		return getGuild().createRole(RoleCreateSpec.create().withColor(Utils.randomColor()).withName(name).withPermissions(readSend));
	}

	protected Mono<Void> deleteRole(long id) {
		return getGuild().getRoleById(Snowflake.of(id)).flatMap(role -> role.delete("Bot request to remove")).then();
	}

	protected static boolean hasRole(Member member, long roleId) {
		return member.getRoleIds().contains(Snowflake.of(roleId));
	}

}
