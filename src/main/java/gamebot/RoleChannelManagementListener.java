package gamebot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

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
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Custom;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import meetup.MeetupLinker;
import reactor.util.Logger;
import reactor.util.Loggers;

public class RoleChannelManagementListener extends CoreHelpers {

	private interface Command {
		void execute(String command);
	}

	private static Logger log = Loggers.getLogger("logger");
	private long ADD_GENRES_HERE = 731850699889049600L;

	private HashMap<String, Command> commands = new HashMap<>();
	private HashMap<String, Long> genreRoles = new HashMap<>();

	private PermissionSet readSend = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES,
			Permission.READ_MESSAGE_HISTORY);

	public void onReady(ReadyEvent event) {
		init(event);
		readDataIntoMap(genreRoles, "genres");
		setupReactsOnGenres();
		initialiseCommands();
	}

	public void onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode()) {
			checkIfDisablingTestMode(event);
			return;
		}
		
		Message message = event.getMessage();
		Channel chn = message.getChannel().block();
		if(chn instanceof PrivateChannel)
			return; // discard pm
		
		log.info("MessageCreateEvent fired for Role Listener");		
		String msg = message.getContent().orElse("");
		Member usr = message.getAuthorAsMember().block();

		if (usr.isBot())
			return;

		if (chn.getId().asLong() == CONSOLE) {
			parseConsole(msg);
		}
	}

	public void onReact(ReactionAddEvent event) {
		if (Utils.isTestingMode())
			return;

		if (event.getUser().block().isBot())
			return;

		if (event.getChannelId().asLong() == ADD_GENRES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			genreRoleToUser(event.getUser().block(), emoji.getName(), true);
		}
	}

	public void onUnreact(ReactionRemoveEvent event) {
		if (Utils.isTestingMode())
			return;

		if (event.getUser().block().isBot())
			return;

		if (event.getChannelId().asLong() == ADD_GENRES_HERE) {
			Custom emoji = event.getEmoji().asCustomEmoji().get();
			genreRoleToUser(event.getUser().block(), emoji.getName(), false);
		}
	}

	private void initialiseCommands() {
		commands.put("!clear", msg -> clearChannel());
		commands.put("!sync", msg -> {
			readDataIntoMap(genreRoles, "genres");		
			setupReactsOnGenres();
			MeetupLinker.readVerified();
			sendMessage(CONSOLE, "Re-synchronised genre and verified lists!");
		});
		commands.put("!add-role", msg -> sendMessage(CONSOLE,
				"Role " + msg + " of ID " + createRole(msg).getId().asLong() + " created!"));
		commands.put("!test", msg -> {
			Utils.flipTestMode();
			sendMessage(CONSOLE,
					"Testing mode is now enabled. You can now run a local copy of the bot and have it perform actions.");
		});
	}

	private String trimCommand(String string) {
		String[] data = string.split("\\s");
		String[] trimmed = Arrays.copyOfRange(data, 1, data.length);
		return String.join(" ", trimmed).trim();
	}

	private void readDataIntoMap(HashMap<String, Long> map, String fileName) {
		map.clear();
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), Charset.defaultCharset());
			for (String line : lines) {
				String[] data = line.split(" ");
				map.put(data[0], new Long(data[1]));
			}
		} catch (IOException e) {
			log.warn("Failed to read file");
		}
	}

	private void setupReactsOnGenres() {
		TextChannel chn = getChannel(ADD_GENRES_HERE);
		Message msg = chn.getLastMessage().block();
		Iterator<?> it = genreRoles.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			msg.addReaction(ReactionEmoji.custom(getEmojiByName((String) pair.getKey()))).block();
		}
	}

	// Console Parsing
	private void parseConsole(String message) {
		String command = message.split("\\s")[0];
		Command toExec = commands.get(command);
		if (toExec != null) {
			toExec.execute(trimCommand(message));
		}
	}

	private void clearChannel() {
		TextChannel chn = getChannel(CONSOLE);
		Snowflake lastMsg = chn.getLastMessageId().get();
		chn.bulkDelete(chn.getMessagesBefore(lastMsg).map(m -> m.getId())).blockFirst();
	}

	private void checkIfDisablingTestMode(MessageCreateEvent event) {
		Message message = event.getMessage();
		Channel chn = message.getChannel().block();
		if (chn.getId().asLong() == CONSOLE && message.getContent().get().equals("!test")) {
			Utils.flipTestMode();
			sendMessage(CONSOLE,
					"Testing mode is now disabled. Be sure to not be running the bot locally to avoid action duplication.");
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

}
