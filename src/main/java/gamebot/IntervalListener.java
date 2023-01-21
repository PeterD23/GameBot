package gamebot;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import meetup.MeetupEvent;
import meetup.MeetupEventManager;
import meetup.MeetupLinker;
import meetup.Pair;
import meetup.SeleniumDriver;
import onlineevent.EventManager;
import onlineevent.OnlineEvent;
import reactor.util.Logger;
import reactor.util.Loggers;

public class IntervalListener extends CoreHelpers {

	private boolean panic = false;
	private static Logger log = Loggers.getLogger("logger");
	
	private long MEETUP = 732273589432090678L;
	private HashMap<String, Command> commands = new HashMap<>();
	private String playlist = "1xfucmjxRtcxXolfNaaA5M";

	private final String prependData = "Here is an upcoming event:\n>>> ";
	
	private static SeleniumDriver driver;
	
	public void onReady(ReadyEvent event) {
		init(event);
		initialiseCommands();
		driver = SeleniumDriver.getInstance();
	}

	public void onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode()) {
			return;
		}

		Message message = event.getMessage();
		Channel chn = message.getChannel().block();
		if(chn instanceof PrivateChannel)
			return; // discard this
		
		log.info("MessageCreateEvent fired for Interval Listener");	
		String msg = message.getContent();
		Member usr = message.getAuthorAsMember().block();

		if (usr.isBot())
			return;

		if (chn.getId().asLong() == CONSOLE) {
			parseConsole(msg);
		}
	}

	private String trimCommand(String string) {
		String[] data = string.split("\\s");
		String[] trimmed = Arrays.copyOfRange(data, 1, data.length);
		return String.join(" ", trimmed).trim();
	}

	// Console Parsing
	private void parseConsole(String message) {
		String command = message.split("\\s")[0];
		Command toExec = commands.get(command);
		if (toExec != null) {
			toExec.execute(trimCommand(message));
		}
	}

	private void initialiseCommands() {
		commands.put("!set-playlist", msg -> setPlaylist(msg));
		commands.put("!recommend", msg -> recommendSong());
		commands.put("!fetch-events", msg -> fetchEventData());
		commands.put("!panic", msg -> panic());
		commands.put("!unlock", msg -> unlockDriver());
	}
	
	private void unlockDriver() {
		driver.unlock();
		sendMessage(CONSOLE, "Web Driver is now unlocked. I would investigate since this shouldn't happen.");
	}
	
	private void panic() {
		panic = !panic;
		sendMessage(CONSOLE, "Panic mode is now "+panic);
	}

	private void fetchEventData() {
		log.info("Fetching events from Meetup");
		logMessage("Fetching events from Meetup, time is "+LocalTime.now().toString());
		
		if(driver.isLocked()) {
			log.info("Driver is currently busy, will try again in 15");
			logMessage("Driver attempted to fetch events but is currently busy");
			return;
		}
		ArrayList<MeetupEvent> events = driver.returnEventData();
		logMessage("Found "+events.size()+" events from Meetup");
		for(MeetupEvent event : events) {
			if(event.toString().equals("err"))
				continue;
			String message = prependData + event.toString() + convertAttendees(event.getID());
			String possibleId = MeetupEventManager.hasEvent(event);
			if(possibleId != "") {
				editMessage(MEETUP, possibleId, message);
			} else {
				String messageId = sendMessage(MEETUP, message);
				getChannel(MEETUP).getMessageById(Snowflake.of(new Long(messageId))).block().pin().block();
				MeetupEventManager.addEvent(event.getID(), messageId, event.getDate());
				logMessage("Added new pinned event to Event List");
			}
		}
	}
	
	private String convertAttendees(String eventId) {
		String list = "\n\n";
		ArrayList<Pair<String, String>> attendees = driver.collateAttendees(eventId);
		logMessage("Found "+attendees.size()+" attendees for event "+eventId+" to append");
		for(Pair<String, String> attendee : attendees) {
			Long userId = MeetupLinker.getUserByMeetupId(new Long(attendee.second()));		
			list += attendee.first() + (userId != 0L ? ": "+getUserIfMentionable(userId) : "") +"\n";
		}
		return list;
	}
	
	private void setPlaylist(String msg) {
		sendMessage(CONSOLE, "Set playlist to https://open.spotify.com/playlist/" + msg);
	}

	public void tick() {
		if(panic) {
			return;
		}
		log.info("IntervalListener is currently ticking");

		handleReminders();
		LocalTime time = LocalTime.now();
		if (time.getHour() == 12 && time.getMinute() == 0) {			
			recommendSong();
		} else if(time.getMinute() % 15 == 0) {
			fetchEventData();
			ArrayList<String> pastEvents = MeetupEventManager.scheduleMessagesForDeletion();
			for(String s : pastEvents) {
				log.info("Deleting message ID "+s);
				logMessage("Deleting message ID"+s+" which is an expired event");
				deleteMessage(MEETUP, s);
			}
		}	
	}
	
	private void handleReminders() {
		ArrayList<OnlineEvent> events = EventManager.getEvents();
		log.info("Handling reminders... Event stack is "+events.size());
		for(OnlineEvent event : events) {
			LocalDateTime date = event.getDateTime();
			log.info("Duration of event from now is "+Duration.between(LocalDateTime.now(), date).toMinutes());
			if(Duration.between(LocalDateTime.now(), date).toMinutes() < 30 && !event.checkHalfHourReminder()){		
				event.triggerHalfHourReminder();
				ArrayList<Long> attendees = event.getAttendeesToDM();
				logMessage("Sending reminders to "+attendees.size()+" people for events");
				for(long attendee : attendees) {
					long privateChannel = getUserById(attendee).getPrivateChannel().block().getId().asLong();
					sendPrivateMessage(privateChannel, "You have an event upcoming in less than 30 minutes, check the online events channel for more details!");
				}
			}
		}
	}

	private void recommendSong() {
		log.info("Scheduled recommendation for music");
		logMessage("Time is 12 pm, recommending a song from Spotify");
		if(LocalDateTime.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
			sendMessage(MUSIC, "https://open.spotify.com/track/79ozNtJ4aqVaAav0bqXpji");
			return;
		}
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			sendMessage(MUSIC, song);
		}
	}

}
