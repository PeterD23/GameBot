package gamebot.listeners;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import gamebot.ChannelLogger;
import gamebot.CoreHelpers;
import gamebot.SpotifyHelpers;
import gamebot.Status;
import gamebot.Utils;
import gamebot.commands.ISlashCommand;
import gamebot.commands.SubscribeCommand;
import gamebot.commands.admin.CallbackCommand;
import gamebot.commands.admin.CallbackNoArgCommand;
import gamebot.commands.admin.ReadCommand;
import meetup.api.JwtDTO;
import meetup.api.MeetupApiQuerier;
import meetup.api.MeetupApiResponse;
import meetup.api.RsvpUser;
import meetup.selenium.MeetupEvent;
import meetup.selenium.MeetupEventManager;
import meetup.selenium.MeetupLinker;
import meetup.selenium.SeleniumDriver;
import misc.Birthday;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class AdminListener extends CoreHelpers implements IListener {

	// Selenium Driver Stuff
	private boolean panic = false;
	private int fetchFrequency = 15;
	private long MEETUP = 732273589432090678L;
	private final String prependData = "\nHere is an upcoming event:\n>>> ";
	private static SeleniumDriver driver;

	private String playlist = "1xfucmjxRtcxXolfNaaA5M";
	private static Logger log = Loggers.getLogger("logger");
	private HashMap<String, ISlashCommand> commands = new HashMap<>();

	public void onReady(ReadyEvent event) {
		init(event);
		initialiseCommands();
		driver = SeleniumDriver.getInstance();
	}

	public void onMessage(MessageCreateEvent event) {
		Message message = event.getMessage();
		Member usr = message.getAuthorAsMember().block();

		if (!isAdmin(usr))
			return;

		if (Utils.isTestingMode()) {
			return;
		}

		Channel chn = message.getChannel().block();
		if (chn instanceof PrivateChannel)
			return; // discard pm

		if (usr.isBot())
			return;
	}

	public void onCommand(ChatInputInteractionEvent event) {
		if (Utils.isTestingMode() && event.getCommandName() != "test") {
			return;
		}
		if (commands.get(event.getCommandName()) != null && isAdmin(event.getInteraction().getMember().get())) {
			commands.get(event.getCommandName()).submitCommand(event).block();
		}
	}

	private void initialiseCommands() {
		commands.put("read", ReadCommand.get());
		commands.put("recommend", recommend());
		commands.put("fetch-events", fetchEvents());
		commands.put("deny", deny());
		commands.put("test", test());
		commands.put("sync", sync());
		commands.put("status", status());
		commands.put("clear", clearChannel());
		commands.put("unlock", unlockDriver());
		commands.put("panic", panic());
		commands.put("set-fetch-freq", setFetchFrequency());
		commands.put("set-playlist", setPlaylist());
	}

	private CallbackNoArgCommand recommend() {
		return new CallbackNoArgCommand("recommend", "Force a song recommendation", evt -> evt
				.reply("Alright, I'll recommend another song...").then(Mono.fromRunnable(() -> recommendSong())));
	}

	private CallbackNoArgCommand fetchEvents() {
		return new CallbackNoArgCommand("fetch-events", "Bypass the Interval Tick and Fetch Meetup Events",
				evt -> evt.reply("Fetching Events now...").then(Mono.fromRunnable(() -> fetchEventDataFromApi())));
	}

	private CallbackNoArgCommand deny() {
		return new CallbackNoArgCommand("deny", "Deny admin commands to admins",
				evt -> evt.reply(Utils.flipAdminDenial()).withEphemeral(true));
	}

	private CallbackNoArgCommand test() {
		return new CallbackNoArgCommand("test", "Enable or disable testing mode",
				evt -> evt.reply(Utils.flipTestMode()).withEphemeral(true).then());
	}

	private CallbackNoArgCommand sync() {
		return new CallbackNoArgCommand("sync", "Re-loads files on disk", evt -> {
			SubscribeCommand.get().readDataIntoTuple("genres");
			MeetupLinker.readVerified();
			MeetupEventManager.init();
			return evt.reply("Re-synchronised genre, verified and event lists!").then();
		});
	}

	private CallbackNoArgCommand status() {
		return new CallbackNoArgCommand("status", "View the bot status", evt -> {
			Status.init();
			boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
			if (isWindows) {
				return evt.reply("I'm running on Windows, dumbass.");
			}
			return evt.reply(Status.getStatus());
		});
	}

	private CallbackNoArgCommand clearChannel() {
		return new CallbackNoArgCommand("clear", "Clears a channel", evt -> {
			TextChannel chn = getChannel(CONSOLE);
			Snowflake lastMsg = chn.getLastMessageId().get();
			chn.bulkDelete(chn.getMessagesBefore(lastMsg).map(m -> m.getId())).blockFirst();
			return evt.reply("Deleted the contents of console.").then();
		});
	}

	private CallbackNoArgCommand unlockDriver() {
		return new CallbackNoArgCommand("unlock", "Unlock the Selenium Driver", evt -> {
			driver.unlock();
			return evt.reply("Web Driver is now unlocked. I would investigate since this shouldn't happen.")
					.withEphemeral(true);
		});
	}

	private CallbackNoArgCommand panic() {
		return new CallbackNoArgCommand("panic", "Disable the interval tick", evt -> {
			panic = !panic;
			return evt.reply("Panic mode is now " + panic).withEphemeral(true);
		});
	}

	private CallbackCommand setFetchFrequency() {
		return new CallbackCommand("set-fetch-freq", "Set the fetching frequency of the Meetup Event").withIntArg("freq", "Frequency in minutes", 5, 90).withCallBack(evt -> {
			int val = evt.getOptionAsLong("freq").get().intValue();
			fetchFrequency = val;
			return evt.reply("Set Meetup Fetch Frequency to " + val + " minutes.");
		}).create();
	}

	private CallbackCommand setPlaylist() {
		return new CallbackCommand("set-playlist", "Set the Spotify Playlist")
				.withStringArg("id", "Id of the playlist", 22, 22)
				.withCallBack(evt -> evt.reply("Set playlist to https://open.spotify.com/playlist/"+evt.getOption("id")).withEphemeral(true)).create();
	}
	
	// ------------- Timed Interval Methods

	public void tick() {
		if (panic) {
			return;
		}
		log.info("IntervalListener is currently ticking");
		LocalTime time = LocalTime.now();
		if (time.getHour() == 12 && time.getMinute() == 0) {
			recommendSong();
			birthdayCheck();
		} else if (time.getMinute() % fetchFrequency == 0) {
			// fetchEventData();
			fetchEventDataFromApi();
			ArrayList<String> pastEvents = MeetupEventManager.scheduleMessagesForDeletion();
			for (String s : pastEvents) {
				log.info("Deleting message ID " + s);
				deleteMessage(MEETUP, s, "Expired Event");
			}
		}
	}

	private void birthdayCheck() {
		LocalDate today = LocalDate.now();
		log.info("Scheduled check for birthdays");
		ArrayList<Long> birthdays = Birthday.hasBirthdaysToday();
		for (Long user : birthdays) {
			sendMessage(GENERAL, "Happy birthday " + getUserIfMentionable(user.longValue()));
		}
		if (today.getMonthValue() == 5 && today.getDayOfMonth() == 23) {
			sendMessage(GENERAL, "Happy Birthday " + getUserById(97036843924598784L).getMention()
					+ "! Please wish him a happy birthday which I totally thought up myself and wasn't hastily added to my programming by him 5 days prior");
		}
		if (today.getMonthValue() == 7 && today.getDayOfMonth() == 11) {
			sendMessage(GENERAL, "It is July 11th, my birthday today! Today marks " + (today.getYear() - 2020)
					+ " years since I first joined the server.");
		}
	}

	private void recommendSong() {
		log.info("Scheduled recommendation for music");
		ChannelLogger.logMessageInfo("Time is 12 pm, recommending a song from Spotify");
		if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
			sendMessage(MUSIC, "https://open.spotify.com/track/79ozNtJ4aqVaAav0bqXpji");
			return;
		}
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			sendMessage(MUSIC, song);
		}
	}

	private void sendMessageIfValid(MeetupEvent event, String content) {
		boolean validDate = !event.getDate().equals("err");
		if (!validDate) {
			ChannelLogger.logMessageError(
					"Unable to parse the date for event " + event.getID() + ", using placeholder date.");
		}
		Message message = sendMessage(MEETUP, content).block();
		pinMessage(MEETUP, message.getId()).block();
		MeetupEventManager.addEvent(event.getID(), message.getId().asString(), validDate ? event.getDate() : "2050-01-01T00:00");
		ChannelLogger.logMessageInfo("Added new pinned event to Event List");
	}

	private String mapAttendees(ArrayList<RsvpUser> attendees) {
		String list = "\n\n";
		ChannelLogger.logMessageInfo("Found " + attendees.size() + " attendees for event to append");
		for (RsvpUser attendee : attendees) {
			long userId = MeetupLinker.getUserByMeetupId(Long.parseLong(attendee.getId()));
			list += attendee.getName().split(" ")[0] + (userId != 0L ? ": " + getUserIfMentionable(userId) : "") + "\n";
		}
		return list;
	}

	private void fetchEventDataFromApi() {
		log.info("Fetching events from Meetup");
		ChannelLogger.logMessageInfo("Fetching events from Meetup API, time is " + LocalTime.now().toString());
		MeetupApiQuerier meetupApi = new MeetupApiQuerier();
		JwtDTO token = meetupApi.generateApiToken();

		if (driver.isLocked()) {
			log.info("Driver is currently busy, will try again in 15");
			ChannelLogger.logMessageWarning("Driver attempted to fetch events but is currently busy");
			return;
		}
		ArrayList<String> eventIds = driver.getEventIds();
		ChannelLogger.logMessageInfo("Found " + eventIds.size() + " events from Meetup");
		for (String eventId : eventIds) {
			MeetupApiResponse event = meetupApi.getEventDetails(token, eventId);
			String message = getEveryoneMention() + prependData + event.toString() + mapAttendees(event.getUsers());
			String possibleId = MeetupEventManager.hasEvent(eventId);
			if (possibleId != "") {
				editMessage(MEETUP, possibleId, message);
			} else {
				sendMessageIfValid(eventId, event, message);
			}
		}
	}

	private void sendMessageIfValid(String eventId, MeetupApiResponse event, String content) {
		Message message = sendMessage(MEETUP, content).block();
		pinMessage(MEETUP, message.getId()).block();
		MeetupEventManager.addEvent(eventId, message.getId().asString(), event.getDateTime());
		ChannelLogger.logMessageInfo("Added new pinned event to Event List");
	}

	// Archived for documentation purposes in case Meetup fuckin make the API
	// unusable
	@SuppressWarnings("unused")
	private void fetchEventData() {
		log.info("Fetching events from Meetup");
		ChannelLogger.logMessageInfo("Fetching events from Meetup, time is " + LocalTime.now().toString());

		if (driver.isLocked()) {
			log.info("Driver is currently busy, will try again in 15");
			ChannelLogger.logMessageWarning("Driver attempted to fetch events but is currently busy");
			return;
		}
		ArrayList<MeetupEvent> events = driver.returnEventData();
		ChannelLogger.logMessageInfo("Found " + events.size() + " events from Meetup");
		for (MeetupEvent event : events) {
			if (event.toString().equals("err"))
				continue;
			String message = getEveryoneMention() + prependData + event.toString();
			String possibleId = MeetupEventManager.hasEvent(event);
			if (possibleId != "") {
				editMessage(MEETUP, possibleId, message);
			} else {
				sendMessageIfValid(event, message);
			}
		}
	}
}
