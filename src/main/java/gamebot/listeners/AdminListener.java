package gamebot.listeners;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import gamebot.ChannelLogger;
import gamebot.CoreHelpers;
import gamebot.EvgIds;
import gamebot.GameBot;
import gamebot.SpotifyHelpers;
import gamebot.Status;
import gamebot.Utils;
import gamebot.commands.ISlashCommand;
import gamebot.commands.SubscribeCommand;
import gamebot.commands.TrustCommand;
import gamebot.commands.admin.CallbackCommand;
import gamebot.commands.admin.CallbackNoArgCommand;
import gamebot.commands.admin.ReadCommand;
import meetup.api.JwtDTO;
import meetup.api.MeetupApiQuerier;
import meetup.api.MeetupApiResponse;
import meetup.selenium.MeetupEventManager;
import meetup.selenium.MeetupLinker;
import meetup.selenium.SeleniumDriver;
import misc.Birthday;
import misc.MessageCache;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import trustsystem.RecommendedActions;

public class AdminListener extends CoreHelpers implements IListener {

	// Selenium Driver Stuff
	private boolean panic = false;
	private int fetchFrequency = 1;
	private static SeleniumDriver driver;

	private String playlist = "1xfucmjxRtcxXolfNaaA5M";
	private static Logger log = Loggers.getLogger("logger");
	private HashMap<String, ISlashCommand> commands = new HashMap<>();

	public Mono<Void> onReady(GuildCreateEvent event) {
		return init(event).then(Mono.fromRunnable(() -> initialiseCommands()))
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
		commands.put("feed-cookie", feedGameBotACookie());
		commands.put("build-cache", buildCache());
		commands.put("see-trust", new TrustCommand(true));

		GatewayDiscordClient client = GameBot.gateway;
		registerCommand(new TrustCommand(true));
	
		client.getRestClient().getApplicationId().map(applicationId -> 
			Flux.fromIterable(commands.entrySet())
			.map(entry -> entry.getValue().getCommandRequest())
			.collectList()
			.map(list -> client.getRestClient()
				.getApplicationService()
				.bulkOverwriteGuildApplicationCommand(applicationId, GameBot.SERVER, list)))
		.then(ChannelLogger.logMessageInfo("Admin Commands successfully registered!")).subscribe();
	}
	
	private void registerCommand(ISlashCommand command) {
		GatewayDiscordClient client = GameBot.gateway;
		client.getRestClient().getApplicationId().log(Loggers.getLogger("Register Command"))
		.map(applicationId -> client.getRestClient()
				.getApplicationService()
				.createGuildApplicationCommand(applicationId, GameBot.SERVER, command.getCommandRequest()).subscribe()
		).subscribe();
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
			return evt.deferReply().then(SubscribeCommand.get().readGenres()).then(MeetupLinker.readVerified())
					.then(MeetupEventManager.init())
					.then(evt.editReply("Re-synchronised genre, verified and event lists!")).then();
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
			return getChannel(EvgIds.CONSOLE_CHANNEL.id()).flatMap(chn -> {
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
				});
	}

	private CallbackCommand setPlaylist() {
		return new CallbackCommand("set-playlist", "Set the Spotify Playlist")
				.withStringArg("id", "Id of the playlist", 22, 22).withCallBack(
						evt -> evt.reply("Set playlist to https://open.spotify.com/playlist/" + evt.getOption("id"))
								.withEphemeral(true));
	}

	private CallbackCommand feedGameBotACookie() {
		return new CallbackCommand("feed-cookie", "Refresh the Meetup Login Session Cookie")
				.withStringArg("token", "JWT Bearer Token", 450, 450).withCallBack(evt -> {
					String token = evt.getOptionAsString("token").get();
					return evt.deferReply().then(Mono.fromSupplier(() -> driver.refreshCookie(token)))
							.flatMap(str -> evt.editReply(str)).then();
				});
	}

	private CallbackNoArgCommand buildCache() {
		return new CallbackNoArgCommand("build-cache", "Generate a message cache of all users", evt -> {
			return evt.reply("Generating cache, this may take a while...").then(MessageCache.cacheUsers(guild))
					.then(evt.createFollowup("Completed!")).then();
		});
	}

	public Mono<?> onCommand(ButtonInteractionEvent event) {
		String[] id = event.getCustomId().split("_");
		String userId = event.getUser().getId().asString();
		String meetupId = MeetupLinker.getMeetupUser(userId);
		if (id[0].startsWith("ts.")) {
			return RecommendedActions.invokeAdminOption(id);
		}
		if (!id[0].startsWith("rsvp")) {
			return Mono.empty();
		}
		if (meetupId.isEmpty()) {
			return event.reply(
					"You aren't Meetup Verified so you cannot use this feature! Use `/link-meetup` to associate your Meetup account with Discord!")
					.withEphemeral(true);
		}

		MeetupApiQuerier meetupApi = new MeetupApiQuerier();
		return event.deferReply().withEphemeral(true)
				.then(Mono.fromCallable(() -> meetupApi.generateApiToken()).flatMap(token -> {
					String[] userData = meetupApi.getNameAndImageOfUser(token, meetupId);
					MeetupApiResponse meetupEvent = meetupApi.getEventDetails(token, id[1]);
					if (meetupEvent.getUsers().stream()
							.anyMatch(user -> user.getId().equals(String.valueOf(meetupId)))) {
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
			return fetchEventDataFromApi().then(MeetupEventManager.scheduleMessagesForDeletion())
					.flatMapMany(list -> Flux.fromIterable(list))
					.flatMap(pastEvent -> ChannelLogger.logMessageInfo("Deleting Past Event ID " + pastEvent).then(
							deleteMessage(EvgIds.MEETUP_CHANNEL.id(), Long.parseLong(pastEvent), "Expired Event")))
					.then();
		}
		return Mono.empty();
	}

	private Mono<?> birthdayCheck() {
		long general = EvgIds.GENERAL_CHANNEL.id();
		LocalDate today = LocalDate.now();
		log.info("Scheduled check for birthdays");
		return Mono.when(Flux.fromIterable(Birthday.hasBirthdaysToday()).flatMap(user -> {
			return sendMessage(general, "Happy birthday " + getUserMention(user));
		})).then(Mono.fromRunnable(() -> {
			if (today.getMonthValue() == 5 && today.getDayOfMonth() == 23) {
				sendMessage(general, "Happy Birthday " + mentionMe()
						+ "! Please wish him a happy birthday which I totally thought up myself and wasn't hastily added to my programming by him 5 days prior")
						.then();
			}
			if (today.getMonthValue() == 7 && today.getDayOfMonth() == 11) {
				sendMessage(general, "It is July 11th, my birthday today! Today marks " + (today.getYear() - 2020)
						+ " years since I first joined the server.").then();
			}
		}));
	}

	private Mono<Message> recommendSong() {
		long music = EvgIds.MUSIC_CHANNEL.id();
		log.info("Scheduled recommendation for music");
		ChannelLogger.logMessageInfo("Time is 12 pm, recommending a song from Spotify");
		if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
			return sendMessage(music, "https://open.spotify.com/track/79ozNtJ4aqVaAav0bqXpji");
		}
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			return sendMessage(music, song);
		}
		return Mono.empty();
	}

	private Mono<Void> fetchEventDataFromApi() {
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
								return tuple.getT2() != "" ? editMessage(EvgIds.MEETUP_CHANNEL.id(),
										Long.parseLong(tuple.getT2()), tuple.getT1().build())
										: sendMessageIfValid(eventId, tuple.getT1());
							})));
		} catch (Exception e) {
			ChannelLogger.logMessageError("Error, unable to get events from Meetup API", e);
			return Mono.empty();
		}
	}

	private Mono<Void> sendMessageIfValid(String eventId, MeetupApiResponse event) {
		return sendMessage(EvgIds.MEETUP_CHANNEL.id(), event.build())
				.flatMap(message -> message.pin().then(Mono.fromRunnable(
						() -> MeetupEventManager.addEvent(message.getId().asString(), eventId, event.getDateTime()))))
				.then(ChannelLogger.logMessageInfo("Added new pinned event to Event List"));
	}
}
