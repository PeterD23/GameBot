package gamebot;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PrivateChannel;
import meetup.EventManager;
import meetup.MeetupEvent;
import meetup.SeleniumDriver;
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
		String msg = message.getContent().orElse("");
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
	}
	
	private void panic() {
		panic = !panic;
		sendMessage(CONSOLE, "Panic mode is now "+panic);
	}

	private void fetchEventData() {
		log.info("Fetching events from Meetup");
		ArrayList<MeetupEvent> events = driver.returnEventData();
		for(MeetupEvent event : events) {
			if(event.toString().equals("err"))
				continue;
			String message = prependData + event.toString();
			String possibleId = EventManager.hasEvent(event);
			if(possibleId != "") {
				editMessage(MEETUP, possibleId, message);
			} else {
				String messageId = sendMessage(MEETUP, message);
				EventManager.addEvent(event.getID(), messageId, event.getDate());
			}
		}
	}
	
	private void setPlaylist(String msg) {
		sendMessage(CONSOLE, "Set playlist to https://open.spotify.com/playlist/" + msg);
	}

	public void tick() {
		if(panic) {
			return;
		}
		log.info("IntervalListener is currently ticking");

		LocalTime time = LocalTime.now();
		if (time.getHour() == 12 && time.getMinute() == 0) {
			recommendSong();
		} else if(time.getMinute() % 15 == 0) {
			fetchEventData();
		}
		ArrayList<String> pastEvents = EventManager.scheduleMessagesForDeletion();
		for(String s : pastEvents) {
			log.info("Deleting message ID "+s);
			deleteMessage(MEETUP, s);
		}
	}

	private void recommendSong() {
		log.info("Scheduled recommendation for music");
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			sendMessage(MUSIC, song);
		}
	}

}
