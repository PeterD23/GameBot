package gamebot;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import reactor.util.Logger;
import reactor.util.Loggers;

public class IntervalListener extends CoreHelpers {

	private static Logger log = Loggers.getLogger("logger");
	private long MUSIC = 797063557341773834L;
	private HashMap<String, Command> commands = new HashMap<>();
	private String playlist = "1xfucmjxRtcxXolfNaaA5M";

	public void onReady(ReadyEvent event) {
		init(event);
		initialiseCommands();
	}

	public void onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode()) {
			return;
		}

		log.info("MessageCreateEvent fired for Interval Listener");

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
	}

	private void setPlaylist(String msg) {
		sendMessage(CONSOLE, "Set playlist to https://open.spotify.com/playlist/" + msg);
	}

	public void tick() {
		log.info("IntervalListener is currently ticking");

		LocalTime time = LocalTime.now();
		if (time.getHour() == 12 && time.getMinute() == 0) {
			log.info("Scheduled recommendation for music");
			recommendSong();
		}
	}

	private void recommendSong() {
		String song = SpotifyHelpers.recommendSong(playlist);
		if (song.length() > 0) {
			sendMessage(MUSIC, song);
		}
	}

}
