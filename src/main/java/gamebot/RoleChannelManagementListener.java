package gamebot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Custom;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

public class RoleChannelManagementListener extends CoreHelpers {

	private interface Command {
		void execute(String command);
	}
	
	private static Logger log = Loggers.getLogger("logger");
	private long CONSOLE = 731604070573408348L;
	private long GAMES_CATEGORY = 731606361368035390L;
	private long ADD_GAMES_HERE = 731608090633306154L;
	private long ADD_GENRES_HERE = 731850699889049600L;
	
	private HashMap<String, Command> commands = new HashMap<>();
	
	private HashMap<String, Long> gameRoles = new HashMap<>();
	private HashMap<String, Long> genreRoles = new HashMap<>();
	
	private PermissionSet readSend = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES,
			Permission.READ_MESSAGE_HISTORY);

	public void onReady(ReadyEvent event) {
		init(event);
		readDataIntoMap(gameRoles, "games");
		readDataIntoMap(genreRoles, "genres");
		setupReactsOnGenres();
		initialiseCommands();
	}
	
	public void onMessage(MessageCreateEvent event) {
		log.info("MessageCreateEvent fired for Role Listener");

		Message message = event.getMessage();
		Channel chn = message.getChannel().block();
		String msg = message.getContent().orElse("");
		Member usr = message.getAuthorAsMember().block();

		if (usr.isBot())
			return;

		if (chn.getId().asLong() == CONSOLE) {
			parseConsole(msg);
		}
	}
	
	public void onReact(ReactionAddEvent event) {
		if(event.getUser().block().isBot())
			return;
		
		if (event.getChannelId().asLong() == ADD_GAMES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			gameRoleToUser(event.getUser().block(), emoji.getName(), true);
		} else if(event.getChannelId().asLong() == ADD_GENRES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			genreRoleToUser(event.getUser().block(), emoji.getName(), true);
		}
	}
	
	public void onUnreact(ReactionRemoveEvent event) {
		if(event.getUser().block().isBot())
			return;
		
		if (event.getChannelId().asLong() == ADD_GAMES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			gameRoleToUser(event.getUser().block(), emoji.getName(), false);
		} else if(event.getChannelId().asLong() == ADD_GENRES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			genreRoleToUser(event.getUser().block(), emoji.getName(), false);
		}
	}
	
	private void initialiseCommands() {
		commands.put("!add-game", msg -> addGame(msg));
		commands.put("!link-react", msg -> addReact(msg.split("\\s")));
		commands.put("!remove-game", msg -> removeGame(msg));
		commands.put("!remove-channel", msg -> addGame(msg));
		commands.put("!clear", msg -> clearChannel());
		commands.put("!sync", msg -> {
			readDataIntoMap(gameRoles, "games");
			readDataIntoMap(genreRoles, "genres");
			sendMessage(CONSOLE, "Re-synchronised game and genre lists!");
		});
		commands.put("!add-role", msg -> sendMessage(CONSOLE, "Role " + msg + " of ID "+createRole(msg).getId().asLong() + " created!"));
	}

	private String trimCommand(String string) {
		String[] data = string.split("\\s");
		String[] trimmed = Arrays.copyOfRange(data, 1, data.length);
		return String.join(" ", trimmed).trim();
	}

	private void readDataIntoMap(HashMap<String, Long> map, String fileName) {
		map.clear();
		try {
			List<String> lines = FileUtils.readLines(new File(fileName));
			for (String line : lines) {
				String[] data = line.split(" ");
				map.put(data[0], new Long(data[1]));
			}
		} catch (IOException e) {
			log.warn("Failed to read file");
		}
	}

	private void saveGameRoles() {
		ArrayList<String> lines = new ArrayList<>();
		for (Map.Entry<String, Long> entry : gameRoles.entrySet()) {
			lines.add(entry.getKey() + " " + entry.getValue());
		}
		try {
			FileUtils.writeLines(new File("games"), lines);
		} catch (IOException e) {
			log.warn("Failed to save games to file");
		}
	}

	private void setupReactsOnGenres() {
		TextChannel chn = getChannel(ADD_GENRES_HERE);
		Message msg = chn.getLastMessage().block();
		msg.addReaction(ReactionEmoji.custom(getEmojiByName("shooter"))).block();
		msg.addReaction(ReactionEmoji.custom(getEmojiByName("strategy"))).block();
		msg.addReaction(ReactionEmoji.custom(getEmojiByName("rpg"))).block();
		msg.addReaction(ReactionEmoji.custom(getEmojiByName("racing"))).block();
		msg.addReaction(ReactionEmoji.custom(getEmojiByName("simulation"))).block();
	}

	// Console Parsing
	private void parseConsole(String message) {
		String command = message.split("\\s")[0];
		Command toExec = commands.get(command);
		if(toExec != null) {
			toExec.execute(trimCommand(message));
		}
	}

	private void clearChannel() {
		TextChannel chn = getChannel(CONSOLE);
		Snowflake lastMsg = chn.getLastMessageId().get();		
		chn.bulkDelete(chn.getMessagesBefore(lastMsg).map(m -> m.getId())).blockFirst();
	}
	
	private void gameRoleToUser(User user, String emojiName, boolean add) {
		Member member = user.asMember(getGuild().getId()).block();
		if (add) {
			member.addRole(Snowflake.of(gameRoles.get(emojiName).longValue())).block();
		} else {
			member.removeRole(Snowflake.of(gameRoles.get(emojiName).longValue())).block();
		}
	}
	
	private void genreRoleToUser(User user, String emojiName, boolean add) {
		Member member = user.asMember(getGuild().getId()).block();
		if (add) {
			member.addRole(Snowflake.of(genreRoles.get(emojiName).longValue())).block();
		} else {
			member.removeRole(Snowflake.of(genreRoles.get(emojiName).longValue())).block();
		}
	}

	private void addReact(String[] data) {
		if (data.length != 2)
			return;
		String channelName = data[0].toLowerCase().replaceAll("\\s", "-");

		// Get Role
		long roleId = gameRoles.get(channelName).longValue();

		// Put emoji into role list
		gameRoles.put(data[1], new Long(roleId));

		// React to Game List
		Message msg = getChannel(ADD_GAMES_HERE).getLastMessage().block();
		GuildEmoji emoji = getGuild().getEmojis().filter(p -> p.getName().equals(data[1])).next().block();
		msg.addReaction(ReactionEmoji.custom(emoji)).block();

		sendMessage(CONSOLE, "Successfully linked emoji to game :) I've reacted to it on the games list so users can now add the game to their list!");
		saveGameRoles();
	}

	private void addGame(String gameName) {
		if (gameName.trim().length() == 0)
			return;

		Guild guild = getGuild();
		Role everyone = guild.getEveryoneRole().block();
		Role gameRole = createRole(gameName);

		String channelName = gameName.toLowerCase().replaceAll("\\s", "-");

		HashSet<PermissionOverwrite> permissions = new HashSet<>();
		permissions.add(PermissionOverwrite.forRole(gameRole.getId(), readSend, PermissionSet.none()));
		permissions.add(PermissionOverwrite.forRole(everyone.getId(), PermissionSet.none(), readSend));

		guild.createTextChannel(chn -> {
			chn.setParentId(Snowflake.of(GAMES_CATEGORY));
			chn.setName(channelName);
			chn.setTopic(gameName);
			chn.setPosition(2);
			chn.setPermissionOverwrites(permissions);
		}).block();

		gameRoles.put(channelName, new Long(gameRole.getId().asLong()));
		
		sendMessage(CONSOLE, gameName + " successfully created! :)");
		saveGameRoles();
	}

	private void removeGame(String gameName) {
		if (gameName.trim().length() == 0)
			return;
		String channelName = gameName.toLowerCase().replaceAll("\\s", "-");

		// Delete role
		long roleId = gameRoles.get(channelName).longValue();
		deleteRole(roleId);
		gameRoles.remove(channelName);

		// Delete channel
		deleteChannel(channelName);

		sendMessage(CONSOLE, "Removed game from list! You'll need to manually remove the reaction if there is one.");
		saveGameRoles();
	}

}
