package gamebot;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.util.Logger;
import reactor.util.Loggers;

public class CoreHelpers {

	protected long CONSOLE = 731604070573408348L;
	protected long SERVER = 731597823640076319L;
	protected long BOT_ID = 731598251437981717L;
	protected long LOG = 902582146437349456L;

	protected long MUSIC = 797063557341773834L;
	protected long EVENTS = 907696207508406342L;

	protected long ADMIN_ROLE = 731604497435983992L;

	private GatewayDiscordClient cli;

	private PermissionSet readSend = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES,
			Permission.READ_MESSAGE_HISTORY);

	private static Logger log = Loggers.getLogger("logger");

	protected void init(ReadyEvent event) {
		cli = event.getClient();
		cli.edit(spec -> {
			spec.setUsername("Game Bot");
		}).block();
	}

	protected Guild getGuild() {
		return cli.getGuildById(Snowflake.of(SERVER)).block();
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
	
	protected boolean mentionedBot(String message) {
		String bot = getUserById(BOT_ID).getMention();
		return message.replaceAll("!", "").contains(bot);
	}
	
	protected boolean isAdmin(Member usr) {
		if (Utils.adminsDenied())
			return false;
		return usr.getRoles().any(p -> p.getId().asLong() == ADMIN_ROLE).block().booleanValue();
	}

	protected Role getRoleById(long id) {
		return getGuild().getRoles().filter(p -> p.getId().asLong() == id).next().block();
	}

	protected Role getRoleByName(String name) {
		return getGuild().getRoles().filter(p -> p.getName().equals(name)).next().block();
	}

	protected Member getUserById(long id) {
		return getGuild().getMembers().filter(p -> p.getId().asLong() == id).next().block();
	}
	
	protected String getUserIfMentionable(long id) {
		Member member = getUserById(id);
		return member != null ? member.getMention() : "";
	}

	protected Member convertUserToMember(long id) {
		return cli.getUserById(Snowflake.of(id)).block().asMember(Snowflake.of(SERVER)).block();
	}

	protected void editMessage(long channelId, String messageId, String newMessage) {
		getChannel(channelId).getMessageById(Snowflake.of(messageId)).block().edit(spec -> spec.setContent(newMessage))
				.block();
		logMessage("Editing message ID " + messageId + " with String of length " + newMessage.length());
	}

	protected void deleteMessage(long channelId, String messageId) {
		getChannel(channelId).getMessageById(Snowflake.of(messageId));
	}

	protected void logMessage(String message) {
		log.info(message);
		getChannel(LOG).createMessage(message).block().getId().asString();
	}

	protected String sendMessage(long channelId, String message) {
		return getChannel(channelId).createMessage(message).block().getId().asString();
	}

	protected String embedImage(long channelId, String imageName) {
		char ps = File.separatorChar;
		String filePath = System.getProperty("user.home") + ps + "Pictures" + ps + imageName;
		logMessage("Looking for " + filePath);
		try (FileInputStream fs = new FileInputStream(filePath)) {
			String messageId = getChannel(channelId).createMessage(spec -> {
				spec.addFile(imageName, fs);
			}).block().getId().asString();
			fs.close();
			return messageId;
		} catch (Exception e) {
			logMessage("Image acquisition failure: " + e.getStackTrace()[0]);
			return sendMessage(channelId, "Couldn't find that image, sorry :(");
		}
	}

	protected Message getMessage(long channelId, long messageId) {
		return getChannel(channelId).getMessageById(Snowflake.of(messageId)).block();
	}

	protected String sendPrivateMessage(long channelId, String message) {
		return getPrivateChannel(channelId).createMessage(message).block().getId().asString();
	}

	protected PrivateChannel getPrivateChannel(long id) {
		return (PrivateChannel) cli.getChannelById(Snowflake.of(id)).block();
	}

	protected TextChannel getChannel(long id) {
		return (TextChannel) getGuild().getChannelById(Snowflake.of(id)).block();
	}

	protected void deleteChannel(String channelName) {
		TextChannel channel = (TextChannel) getGuild().getChannels().filter(p -> p.getName().equals(channelName)).next()
				.block();
		channel.delete("Deleted by bot request").block();
	}

	protected Role createRole(String name) {
		Guild guild = getGuild();
		return guild.createRole(r -> {
			r.setColor(Utils.randomColor());
			r.setName(name);
			r.setPermissions(readSend);
		}).block();
	}

	protected void deleteRole(long id) {
		Role r = getGuild().getRoleById(Snowflake.of(id)).block();
		r.delete("Bot request to remove").block();
	}

	protected boolean hasRole(Member member, String roleName) {
		Snowflake id = getRoleByName(roleName).getId();
		return member.getRoleIds().contains(id);
	}

}
