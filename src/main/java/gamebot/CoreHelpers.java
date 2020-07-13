package gamebot;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;

public class CoreHelpers {

	protected long CONSOLE = 731604070573408348L;
	protected long SERVER = 731597823640076319L;
	protected long BOT_ID = 731598251437981717L;

	private DiscordClient cli;
	
	private PermissionSet readSend = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES,
			Permission.READ_MESSAGE_HISTORY);
	
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
	
	protected Role getRoleByName(String name) {
		return getGuild().getRoles().filter(p -> p.getName().equals(name)).next().block();
	}

	protected void sendMessage(long channelId, String message) {
		getChannel(channelId).createMessage(message).block();
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
