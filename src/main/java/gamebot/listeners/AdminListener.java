package gamebot.listeners;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
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
import meetup.selenium.MeetupEvent;
import meetup.selenium.MeetupEventManager;
import meetup.selenium.MeetupLinker;
import meetup.selenium.SeleniumDriver;
import misc.Birthday;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class AdminListener extends CoreHelpers implements IListener {

	// Selenium Driver Stuff
	private boolean panic = false;
	private int fetchFrequency = 1;
	private long MEETUP = 732273589432090678L;
	private final String prependData = "\nHere is an upcoming event:\n>>> ";
	private static SeleniumDriver driver;

	private String playlist = "1xfucmjxRtcxXolfNaaA5M";
	private static Logger log = Loggers.getLogger("logger");
	private HashMap<String, ISlashCommand> commands = new HashMap<>();

	public Mono<Void> onReady(GuildCreateEvent event) {
		return init(event).then(initialiseCommands())
				.then(Mono.fromCallable(() -> driver = SeleniumDriver.getInstance()))
				.flatMap(webdriver -> webdriver.login());
	}

	public Mono<?> onCommand(ChatInputInteractionEvent event) {
		String command = event.getCommandName();
		if (Utils.isTestingMode() && !command.equals("test")) {
			return Mono.empty();
		}
		if (commands.get(command) != null && isAdmin(event.getInteraction().getMember().get())) {
			return commands.get(command).submitCommand(event);
		}
		return Mono.empty();
	}

	private Mono<Void> initialiseCommands() {
		return Mono.fromRunnable(() -> {
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
			commands.put("feed-cookie", feedGameBotACookie());
		});
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
			return getChannel(CONSOLE).flatMap(chn -> {
				Snowflake lastMsg = chn.getLastMessageId().get();
				chn.bulkDelete(chn.getMessagesBefore(lastMsg).map(m -> m.getId())).blockFirst();
				return evt.reply("Deleted the contents of console.").then();
			});
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
		return new CallbackCommand("set-fetch-freq", "Set the fetching frequency of the Meetup Event")
				.withIntArg("freq", "Frequency in minutes", 5, 90).withCallBack(evt -> {
					int val = evt.getOptionAsLong("freq").get().intValue();
					fetchFrequency = val;
					return evt.reply("Set Meetup Fetch Frequency to " + val + " minutes.");
				}).create();
	}

	private CallbackCommand setPlaylist() {
		return new CallbackCommand("set-playlist", "Set the Spotify Playlist")
				.withStringArg("id", "Id of the playlist", 22, 22)
				.withCallBack(
						evt -> evt.reply("Set playlist to https://open.spotify.com/playlist/" + evt.getOption("id"))
								.withEphemeral(true))
				.create();
	}

	private CallbackCommand feedGameBotACookie() {
		return new CallbackCommand("feed-cookie", "Refresh the Meetup Login Session Cookie")
				.withStringArg("token", "JWT Bearer Token", 450, 450).withCallBack(evt -> {
					String token = evt.getOptionAsString("token").get();
					return evt.deferReply().then(Mono.fromSupplier(() -> driver.refreshCookie(token)))
							.flatMap(str -> evt.editReply(str)).then();
				}).create();
	}

	public Mono<?> onCommand(ButtonInteractionEvent event) {
		String[] id = event.getCustomId().split("_");
		long userId = event.getUser().getId().asLong();
		long meetupId = MeetupLinker.getMeetupUser(userId);
		if (!id[0].startsWith("rsvp")) {
			return Mono.empty();
		}
		if (meetupId == 0L) {
			return event.reply(
					"You aren't Meetup Verified so you cannot use this feature! Use `/link-meetup` to associate your Meetup account with Discord!")
					.withEphemeral(true);
		}

		MeetupApiQuerier meetupApi = new MeetupApiQuerier();
		return event.deferReply().withEphemeral(true)
				.then(Mono.fromCallable(() -> meetupApi.generateApiToken()).flatMap(token -> {
					String[] userData = meetupApi.getNameAndImageOfUser(token, meetupId);
					MeetupApiResponse meetupEvent = meetupApi.getEventDetails(token, id[1]);
					if (meetupEvent.getUsers().stream().anyMatch(user -> user.getId().equals(String.valueOf(meetupId)))) {
						return event.editReply("You are already RSVP'd to this event!");
					}
					if (meetupEvent.canRsvp()) {
						try {
							driver.rsvpUser(id[1], userData);
						} catch (Exception e) {
							return event.editReply("I failed to RSVP you, sorry :(");
						}
						meetupEvent = meetupApi.getEventDetails(token, id[1]);
						if (meetupEvent.getUsers().stream()
								.anyMatch(user -> user.getId().equals(String.valueOf(meetupId)))) {
							return event.editReply("Congrats, you are now going to this event!");
						}
					}
					return event.editReply(
							"I'm sorry but this event is now full. You can add yourself to the waitlist from the Meetup event page.");
				}));
	}

	// ------------- Timed Interval Methods

	public Mono<?> tick() {
		if (panic) {
			return Mono.empty();
		}
		log.info("IntervalListener is currently ticking");
		LocalTime time = LocalTime.now();
		if (time.getHour() == 12 && time.getMinute() == 0) {
			return recommendSong().then(birthdayCheck());
		} else if (time.getMinute() % fetchFrequency == 0) {
			return fetchEventDataFromApi().thenMany(Flux.fromIterable(MeetupEventManager.scheduleMessagesForDeletion()))
					.flatMap(pastEvent -> ChannelLogger.logMessageInfo("Deleting Past Event ID " + pastEvent)
							.then(deleteMessage(MEETUP, Long.parseLong(pastEvent), "Expired Event")))
					.then();
		}
		return Mono.empty();
	}

	private Mono<?> birthdayCheck() {
		LocalDate today = LocalDate.now();
		log.info("Scheduled check for birthdays");
		return Mono.when(Flux.fromIterable(Birthday.hasBirthdaysToday()).flatMap(user -> {
			return sendMessage(GENERAL, "Happy birthday " + getUserMention(user.longValue()));
		})).then(Mono.fromRunnable(() -> {
			if (today.getMonthValue() == 5 && today.getDayOfMonth() == 23) {
				sendMessage(GENERAL, "Happy Birthday " + mentionMe()
						+ "! Please wish him a happy birthday which I totally thought up myself and wasn't hastily added to my programming by him 5 days prior")
						.then();
			}
			if (today.getMonthValue() == 7 && today.getDayOfMonth() == 11) {
				sendMessage(GENERAL, "It is July 11th, my birthday today! Today marks " + (today.getYear() - 2020)
						+ " years since I first joined the server.").then();
			}
		}));
	}

	private Mono<Message> recommendSong() {
		log.info("Scheduled recommendation for music");
		ChannelLogger.logMessageInfo("Time is 12 pm, recommending a song from Spotify");
		if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
			return sendMessage(MUSIC, "https://open.spotify.com/track/79ozNtJ4aqVaAav0bqXpji");
		}
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			return sendMessage(MUSIC, song);
		}
		return Mono.empty();
	}

	private Mono<?> fetchEventDataFromApi() {
		log.info("Fetching events from Meetup");
		ChannelLogger.logMessageInfo("Fetching events from Meetup API, time is " + LocalTime.now().toString());
		MeetupApiQuerier meetupApi = new MeetupApiQuerier();
		JwtDTO token = meetupApi.generateApiToken();

		try {
			ArrayList<String> eventIds = meetupApi.getUpcomingEvents(token);
			ChannelLogger.logMessageInfo("Found " + eventIds.size() + " events from Meetup");
			return Mono.when(Flux.fromIterable(eventIds)
					.flatMap(eventId -> Mono.fromCallable(() -> meetupApi.getEventDetails(token, eventId)).flatMap(
							event -> Mono.zip(Mono.just(event), Mono.just(MeetupEventManager.hasEvent(eventId))))
							.flatMap(tuple -> {
								return tuple.getT2() != ""
										? editMessage(LOG, Long.parseLong(tuple.getT2()), tuple.getT1().build())
										: sendMessageIfValid(eventId, tuple.getT1());
							})));
		} catch (Exception e) {
			ChannelLogger.logMessageError("Error, unable to get events from Meetup API", e);
			return Mono.empty();
		}
	}

	private Mono<Void> sendMessageIfValid(String eventId, MeetupApiResponse event) {
		return sendMessage(LOG, event.build())
				.flatMap(message -> message.pin().then(Mono.fromRunnable(
						() -> MeetupEventManager.addEvent(eventId, message.getId().asString(), event.getDateTime()))))
				.then(ChannelLogger.logMessageInfo("Added new pinned event to Event List"));
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
				editMessage(MEETUP, Long.parseLong(possibleId), message);
			} else {
				sendMessageIfValid(event, message);
			}
		}
	}

	private Mono<Void> sendMessageIfValid(MeetupEvent event, String content) {
		boolean validDate = !event.getDate().equals("err");
		if (!validDate) {
			ChannelLogger.logMessageError(
					"Unable to parse the date for event " + event.getID() + ", using placeholder date.",
					new RuntimeException());
		}
		return sendMessage(LOG, content)
				.flatMap(message -> message.pin()
						.then(Mono.fromRunnable(() -> MeetupEventManager.addEvent(event.getID(),
								message.getId().asString(), validDate ? event.getDate() : "2050-01-01T00:00Z"))))
				.then(ChannelLogger.logMessageInfo("Added new pinned event to Event List"));
	}
}
