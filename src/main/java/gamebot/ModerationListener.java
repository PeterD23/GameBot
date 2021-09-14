package gamebot;

import java.util.HashMap;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Member;
import reactor.util.Logger;
import reactor.util.Loggers;

public class ModerationListener extends CoreHelpers {

	private static Logger log = Loggers.getLogger("logger");
	private long INTRODUCTIONS = 732247266173124648L;
	private HashMap<String, UserCommand> commands = new HashMap<>();

	public void onReady(ReadyEvent event) {
		init(event);
		initialiseCommands();
	}

	public void onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode())
			return;

		log.info("MessageCreateEvent fired for Moderation Listener");
		Member usr = event.getMember().get();
		if (usr.isBot())
			return;
		
		String msg = event.getMessage().getContent().orElse("");
		parseCommand(event, msg);
	}

	public void newUser(MemberJoinEvent event) {
		if (Utils.isTestingMode())
			return;

		Member usr = event.getMember();
		introduceYourself(usr, INTRODUCTIONS);
	}

	private void parseCommand(MessageCreateEvent event, String message) {
		String command = message.split("\\s")[0];
		UserCommand toExec = commands.get(command);
		if (toExec != null) {
			toExec.execute(event, message);
		}
	}

	private void initialiseCommands() {
		commands.put("!verify", (evt, msg) -> verify(evt, msg));
		commands.put("!hello",
				(evt, msg) -> introduceYourself(evt.getMember().get(), evt.getMessage().getChannelId().asLong()));
		commands.put("!about", (evt, msg) -> about(evt));
	}

	private void verify(MessageCreateEvent event, String message) {
		Member member = event.getMember().get();
		long channelId = event.getMessage().getChannelId().asLong();

		if (hasRole(member, "Verified")) {
			sendMessage(channelId, "Hello " + member.getMention() + ", you are already verified :heart:");
			return;
		}

		int count = message.split("\\s").length;
		if (count >= 30) {
			member.addRole(getRoleByName("Verified").getId()).block();
			sendMessage(channelId, member.getMention()
					+ " Cool! I've verified you. You now have access to the rest of the server! Use the add-games-here and add-genres-here channels to subscribe to the games and genres you're interested in!");
		} else {
			sendMessage(channelId,
					"Hi, unfortunately that introduction wasn't quite long enough. Remember, 30 words or more!");
		}
	}

	private void introduceYourself(Member member, long channelId) {
		if (hasRole(member, "Verified")) {
			sendMessage(channelId, "Hello " + member.getMention() + "! " + Utils.getARandomGreeting());
		} else {
			sendMessage(channelId, "Hello " + member.getMention()
					+ "! Welcome to the server! You'll need to get verified before you can post in other channels, so type !verify followed by a short (30 word) introduction about yourself, and your meetup name.");
		}
	}

	private void about(MessageCreateEvent event) {
		long channelId = event.getMessage().getChannelId().asLong();
		sendMessage(channelId,
				"Hi! I'm Game Bot, and I help out managing peoples roles on the server! All my inner workings were done by "
						+ getUserById(97036843924598784L).getMention()
						+ " so if at any point I stop working, please give him a kick!");
	}
}
