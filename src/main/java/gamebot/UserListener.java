package gamebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import meetup.MeetupLinker;
import meetup.SeleniumDriver;
import onlineevent.EventManager;
import onlineevent.OnlineEvent;
import onlineevent.Poll;
import reactor.util.Logger;
import reactor.util.Loggers;

public class UserListener extends CoreHelpers {

	private static Logger log = Loggers.getLogger("logger");
	private long INTRODUCTIONS = 732247266173124648L;
	private long MEETUP_VERIFIED = 902260032945651774L;
	private HashMap<String, UserCommand> commands = new HashMap<>();

	private LocalDateTime restrictedPoll = LocalDateTime.now();

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

	public void onEdit(MessageUpdateEvent event) {
		if (Utils.isTestingMode())
			return;

		Message message = event.getMessage().block();
		long channelId = message.getChannel().block().getId().asLong();

		log.info("MessageCreateEvent fired for Moderation Listener");
		Member usr = message.getAuthorAsMember().block();
		if (usr.isBot())
			return;

		String msg = message.getContent().orElse("");
		if (msg.startsWith("!verify"))
			verify(usr, channelId, msg);
	}

	public void onReact(ReactionAddEvent event) {
		if (Utils.isTestingMode())
			return;

		Member usr = event.getMember().get();
		if (usr.isBot())
			return;

		if (event.getChannelId().asLong() == EVENTS) {
			ArrayList<OnlineEvent> events = EventManager.getEvents();
			Message msg = event.getMessage().block();
			OnlineEvent online = events.stream().filter(p -> p.getMessageId() == msg.getId().asLong()).findFirst()
					.orElse(null);
			if (online == null) {
				logMessage("ERROR: An event message was reacted to but with no associated event.");
				return;
			}
			online.addAttendee(usr.getMention());
			Message message = getMessage(EVENTS, online.getMessageId());
			message.edit(spec -> {
				spec.setContent(online.toString());
			}).block();
			EventManager.saveEventData();
		}
	}

	public void onUnreact(ReactionRemoveEvent event) {
		if (Utils.isTestingMode())
			return;

		User usr = event.getUser().block();
		if (usr.isBot())
			return;

		if (event.getChannelId().asLong() == EVENTS) {
			ArrayList<OnlineEvent> events = EventManager.getEvents();
			Message msg = event.getMessage().block();
			OnlineEvent online = events.stream().filter(p -> p.getMessageId() == msg.getId().asLong()).findFirst()
					.orElse(null);
			if (online == null) {
				logMessage("ERROR: An event message was reacted to but with no associated event.");
				return;
			}
			online.removeAttendee(usr.getMention());
			Message message = getMessage(EVENTS, online.getMessageId());
			message.edit(spec -> {
				spec.setContent(online.toString());
			}).block();
			EventManager.saveEventData();
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
		commands.put("!verify", (evt, msg) -> createMsgVerify(evt, msg));
		commands.put("!hello",
				(evt, msg) -> introduceYourself(evt.getMember().get(), evt.getMessage().getChannelId().asLong()));
		commands.put("!about", (evt, msg) -> about(evt));
		commands.put("!link-meetup", (evt, msg) -> linkMeetup(evt));
		commands.put("!poll", (evt, msg) -> createPoll(evt, msg));
		commands.put("!event", (evt, msg) -> createEvent(evt, msg));
		commands.put("!help", (evt, msg) -> help(evt, msg));
		commands.put("!bash", (evt, msg) -> bash(evt, msg));
		commands.put("!embed", (evt, msg) -> embed(evt, msg));
		commands.put("!watch-poll", (evt, msg) -> watch(evt, "Poll"));
		commands.put("!watch-event", (evt, msg) -> watch(evt, "Event"));
		commands.put("!selfie", (evt, msg) -> selfie(evt));
		commands.put("!hltb", (evt, msg) -> howLongToBeat(evt, msg));
		commands.put("!reset", (evt, msg) -> resetPoll(evt));
	}

	private void howLongToBeat(MessageCreateEvent event, String game) {
		JsonNode output = null;
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost post = new HttpPost("http://localhost:2460/game");
			String JSON_STRING = "{ \"game\":\"" + game.replace("!hltb", "").trim() + "\" }";
			HttpEntity stringEntity = new StringEntity(JSON_STRING, ContentType.APPLICATION_JSON);
			post.setEntity(stringEntity);
			try (CloseableHttpResponse response = httpClient.execute(post)) {
				HttpEntity responseEntity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == 200) {
					String json = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
					ObjectMapper mapper = Utils.buildObjectMapper();
					output = mapper.readTree(json);
				}
			}
		} catch (Exception e) {
			logMessage("HTTP Post failed with Error: " + e.getStackTrace()[0]);
			e.printStackTrace();
		}
		long channel = event.getMessage().getChannelId().asLong();
		postDataToChannel(output, channel);
	}

	private void postDataToChannel(JsonNode output, long channelId) {
		if (output == null) {
			sendMessage(channelId, "An error occurred. Please type a valid game!");
			return;
		}
		String name = output.findValue("name").asText();
		String timeToBeatMain = "Time to Beat Campaign: " + output.findValue("gameplayMain").asText() + " Hours";
		String timeToBeatExtra = "Time to Beat Extras: " + output.findValue("gameplayMainExtra").asText() + " Hours";
		String timeToBeatCompletion = "Time to 100%: " + output.findValue("gameplayCompletionist").asText() + " Hours";
		String openCriticScore = "OpenCritic Score: N/A";

		String openCriticName = Iterables.get(output.findValues("name"), 1).asText();
		logMessage("Comparing Name:'" + name + "' to OpenCritic Name:'" + openCriticName + "'");
		if (name.equals(openCriticName)) {
			openCriticScore = "OpenCritic Score: " + output.findValue("medianScore").asText();
		}
		String image = name.replaceAll("\\s|\\:|\\,", "").trim().toLowerCase();
		Utils.downloadJPG("https://howlongtobeat.com" + output.findValue("imageUrl").asText(), image, 100);
		sendMessage(channelId, "**" + name + "**");
		embedImage(channelId, image + ".jpg");
		sendMessage(channelId, Utils.constructMultiLineString(1, timeToBeatMain, timeToBeatExtra, timeToBeatCompletion,
				openCriticScore));
	}

	private void watch(MessageCreateEvent event, String type) {
		long chn = event.getMessage().getChannelId().asLong();
		String role = type + " Watcher";
		Member usr = event.getMember().get();
		if (!hasRole(usr, role)) {
			usr.addRole(getRoleByName(role).getId(), "Requested by user").block();
			sendMessage(chn, "Hey " + usr.getMention() + " you will now be pinged whenever a new " + type.toLowerCase()
					+ " is created!");
		} else {
			usr.removeRole(getRoleByName(role).getId(), "Requested by user").block();
			sendMessage(chn, "Hey " + usr.getMention() + " you will no longer be pinged whenever a new "
					+ type.toLowerCase() + " is created!");
		}
	}

	private void selfie(MessageCreateEvent event) {
		long chn = event.getMessage().getChannelId().asLong();
		executeBash("cd && sudo ./selfie.sh");
		embedImage(chn, "selfie.jpg");
	}

	private void embed(MessageCreateEvent event, String image) {
		long chn = event.getMessage().getChannelId().asLong();
		embedImage(chn, image.replace("!embed", "").trim());
	}

	private void createPoll(MessageCreateEvent event, String message) {
		Member usr = event.getMember().get();
		long chn = event.getMessage().getChannelId().asLong();
		Duration time = checkPollRestricted(usr);
		if (!time.isNegative()) {
			sendMessage(chn,
					"Hey, sorry but you'll need to wait until you can create another poll! You should be allowed to create another in "
							+ time.toMinutes() + " minutes");
			return;
		}
		Poll poll = new Poll(message);
		String id = sendMessage(chn, getRoleByName("Poll Watcher").getMention() + "\n\n" + poll.printPoll());
		Message pollMsg = getMessage(chn, new Long(id));
		pollMsg.pin().block();
		poll.react(pollMsg);
		restrictPoll(usr, LocalDateTime.now().plusMinutes(15));
	}

	private void createEvent(MessageCreateEvent event, String message) {
		Member usr = event.getMember().get();
		long chn = event.getMessage().getChannelId().asLong();
		Duration time = checkPollRestricted(usr);
		if (!time.isNegative()) {
			sendMessage(chn,
					"Hey, sorry but you'll need to wait until you can create another event! You should be allowed to create another in "
							+ time.toMinutes() + " minutes");
			return;
		}
		OnlineEvent onlineEvent = new OnlineEvent(usr.getMention(), message);
		if (!onlineEvent.isValidEvent()) {
			sendMessage(chn,
					"Sorry about that, I was unable to create the event :( Your time formatting should be yyyy-MM-dd HH:mm.");
			return;
		}
		String messageId = sendMessage(EVENTS,
				getRoleByName("Event Watcher").getMention() + "\n\n" + onlineEvent.toString());
		onlineEvent.addMessageId(new Long(messageId));
		Message eventMsg = getMessage(chn, new Long(messageId));
		eventMsg.pin().block();
		eventMsg.addReaction(ReactionEmoji.unicode("\u2705")).block();
		EventManager.add(onlineEvent);
		restrictPoll(usr, LocalDateTime.now().plusMinutes(15));
		sendMessage(chn, "There you go " + usr.getMention() + ", I've created a new event for you! ^_^");
	}

	private void help(MessageCreateEvent event, String msg) {
		long chn = event.getMessage().getChannelId().asLong();
		if (msg.contains("poll")) {
			sendMessage(chn,
					"To create a poll, type !poll followed by the description of the poll. By default it will use Yes/No, but if you want custom answers define as so: (Yes|No|Other|Aliens|Cheese)");
		} else if (msg.contains("event")) {
			sendMessage(chn,
					"To create an event, type !event followed by the following format, using | as delimiters: TITLE | DESCRIPTION | DATETIME(yyyy-MM-dd HH:mm)");
		} else {
			sendMessage(chn,
					"Hi there! You can use the following commands: !hello, !about, !link-meetup, !poll, !event, !watch-poll, !watch-event");
		}
	}

	private void bash(MessageCreateEvent event, String msg) {
		long chn = event.getMessage().getChannelId().asLong();
		if (!isAdmin(event.getMember().get()))
			return;
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		if (isWindows) {
			sendMessage(chn, "I'm running on Windows, dumbass.");
			return;
		}
		String output = executeBash(msg.replace("!bash", "").trim());
		sendMessage(chn, output);
	}

	private String executeBash(String command) {
		if (!canExecute(command))
			return "Command has not been whitelisted.";
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("sh", "-c", command);
			builder.directory(new File(System.getProperty("user.home")));
			Process process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			int exitCode = process.waitFor();
			return "```" + sb.toString() + "\nProcess exited with code " + exitCode + "```";
		} catch (IOException | InterruptedException e) {
			logMessage("Bash script failed to execute: " + e.getStackTrace()[0]);
			return "Woops haha that didn't work";
		}
	}

	private boolean canExecute(String command) {
		try {
			List<String> commands = Files.readLines(new File("whitelist"), Charset.defaultCharset());
			return commands.contains(command);
		} catch (IOException e) {
			return false;
		}
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

	private void createMsgVerify(MessageCreateEvent event, String message) {
		Member member = event.getMember().get();
		long channelId = event.getMessage().getChannelId().asLong();
		verify(member, channelId, message);
	}

	private void verify(Member member, long channelId, String message) {
		if (hasRole(member, "Verified")) {
			sendMessage(channelId, "Hello " + member.getMention() + ", you are already verified :heart:");
			return;
		}
		int count = message.split("\\s").length;
		if (count >= 30) {
			member.addRole(getRoleByName("Verified").getId()).block();
			sendMessage(channelId, member.getMention()
					+ " Cool! I've verified you. You now have access to the rest of the server! Use the add-genres-here channel to subscribe to the game genres you're interested in!");
		} else {
			sendMessage(channelId,
					"Hi, unfortunately that introduction wasn't quite long enough. Remember, 30 words or more! You can edit your message instead of making a new one since I have super secret sensing powers!");
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

	protected void restrictPoll(Member usr, LocalDateTime time) {
		if (isAdmin(usr))
			return;
		restrictedPoll = time;
	}

	protected void resetPoll(MessageCreateEvent event) {
		Member usr = event.getMember().get();
		long chn = event.getMessage().getChannelId().asLong();
		if (isAdmin(usr)) {
			restrictedPoll = LocalDateTime.now();
			sendMessage(chn, "https://c.tenor.com/vpGEo_akgVsAAAAd/will-smith-shades.gif");
		} else {
			sendMessage(chn,
					Utils.randomOf("https://c.tenor.com/EHcnfkne6S0AAAAd/shaking-head-colin-jost.gif",
							"https://c.tenor.com/4MhCTwlfFfQAAAAd/john-krasinski-really.gif",
							"https://c.tenor.com/4nO578j2ikcAAAAM/nope.gif",
							"https://c.tenor.com/8wLU9UiDhNMAAAAM/kristen-bell-smh.gif"));
		}
	}

	protected Duration checkPollRestricted(Member usr) {
		return Duration.between(LocalDateTime.now(), isAdmin(usr) ? LocalDateTime.now().minusHours(1) : restrictedPoll);
	}
}
