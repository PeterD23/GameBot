package gamebot;

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import meetup.MeetupLinker;
import meetup.SeleniumDriver;
import reactor.util.Logger;
import reactor.util.Loggers;

public class ModerationListener extends CoreHelpers {

	private static Logger log = Loggers.getLogger("logger");
	private long INTRODUCTIONS = 732247266173124648L;
	private long MEETUP_VERIFIED = 902260032945651774L;
	private HashMap<String, UserCommand> commands = new HashMap<>();

	public void onReady(ReadyEvent event) {
		init(event);
		initialiseCommands();
		MeetupLinker.readVerified();
	}

	public void onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode())
			return;

		Message message = event.getMessage();
		Channel chn = message.getChannel().block();
		if (chn instanceof PrivateChannel) {
			User usr = message.getAuthor().get();
			if (usr.isBot())
				return;
			checkIfVerifyingMeetup(usr, event.getMessage().getContent().orElse(""));
			return;
		}

		log.info("MessageCreateEvent fired for Moderation Listener");
		Member usr = event.getMember().get();
		if (usr.isBot())
			return;

		String msg = event.getMessage().getContent().orElse("");
		parseCommand(event, msg);
		if (chn.getId().asLong() == MUSIC && new Random().nextInt(6) == 5
				&& msg.startsWith("https://open.spotify.com/track/")) {
			sendMessage(MUSIC, "https://c.tenor.com/1S9zA-EMU4YAAAAC/stay-out.gif");
		}
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
		commands.put("!link-meetup", (evt, msg) -> linkMeetup(evt));
	}

	private void checkIfVerifyingMeetup(User usr, String msg) {
		long userId = usr.getId().asLong();
		long channelId = usr.getPrivateChannel().block().getId().asLong();
		if (SeleniumDriver.getInstance().isLocked()) {
			logMessage("User tried to verify identity but browser is currently locked");
			sendPrivateMessage(channelId,
					"The browser is currently busy at the moment, try again in a short while! Can't get the staff these days...");
			return;
		}
		if (MeetupLinker.isQueued(userId)) {
			String code = MeetupLinker.getUsersCode(userId);
			if (msg.startsWith("https://www.meetup.com/members/")) {
				long meetupId = SeleniumDriver.getInstance().sendCode(msg, code);
				MeetupLinker.addMeetupId(userId, meetupId);
				sendPrivateMessage(channelId,
						"Cool! I've sent you a message on Meetup, just respond here with the code.");
			} else if (msg.equals(code)) {
				logMessage("User " + userId + " was successfully added to Meetup Verified list.");
				MeetupLinker.verifyUser(userId);
				sendPrivateMessage(channelId,
						"Hey! You have been successfully verified! Here, have a role on the house!");
				usr.asMember(Snowflake.of(SERVER)).block().addRole(Snowflake.of(MEETUP_VERIFIED)).block();
			} else {
				sendPrivateMessage(channelId,
						"Hi! You appear to have entered something other than the code or a valid meetup URL!");
			}
		}
	}

	private void linkMeetup(MessageCreateEvent event) {
		Member usr = event.getMember().get();
		long usrId = usr.getId().asLong();
		long channelId = usr.getPrivateChannel().block().getId().asLong();
		boolean alreadyQueued = MeetupLinker.queueUser(usrId, RandomStringUtils.randomAlphanumeric(5));
		if (alreadyQueued)
			return;
		sendPrivateMessage(channelId, "Hi there! If you want to link your account to meetup, "
				+ "please respond with your user profile URL in full which should look "
				+ "something like this https://www.meetup.com/members/343387847/"
				+ "\n\nYour privacy settings must be set to allow other members to message you so I can send you the code. This only needs to be done once!");

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
