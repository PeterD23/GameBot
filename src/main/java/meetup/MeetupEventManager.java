package meetup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import reactor.util.Logger;
import reactor.util.Loggers;

public class MeetupEventManager {

	private static Logger log = Loggers.getLogger("logger");
	private static ArrayList<Tuple<String, String, String>> events;
	
	public static void init() {
		events = new ArrayList<>();
		readEventData();		
	}
	
	public static void addEvent(String eventId, String messageId, String timeToDelete) {
		events.add(new Tuple<>(eventId, messageId, timeToDelete));
		saveEventData();
	}
	
	public static ArrayList<String> scheduleMessagesForDeletion() {
		log.info("Scheduling past events for deletion...");
		LocalDateTime time = LocalDateTime.now();
		ArrayList<String> toRemove = events.stream().filter(o -> {
			LocalDateTime check = LocalDateTime.parse(o.third()).plusHours(16);
			return time.compareTo(check) > 0; 
		}).map(o -> o.second()).collect(Collectors.toCollection(ArrayList::new));
		events.removeIf(o -> toRemove.contains(o.second()));
		log.info("Found "+toRemove.size()+ ", Events list now contains "+events.size());
		saveEventData();
		return toRemove;
	}
	
	private static void readEventData() {
		events.clear();
		try {
			List<String> lines = FileUtils.readLines(new File("events"), Charset.defaultCharset());
			for (String line : lines) {
				String[] data = line.split(" ");
				events.add(new Tuple<>(data[0], data[1], data[2]));
			}
		} catch (IOException e) {
			log.warn("Failed to read file");
		}
	}
	
	private static void saveEventData() {
		ArrayList<String> lines = new ArrayList<>();
		for (Tuple<String, String, String> event : events) {
			lines.add(String.join(" ", event.first(), event.second(), event.third()));
		}
		try {
			FileUtils.writeLines(new File("events"), lines);
		} catch (IOException e) {
			log.warn("Failed to save games to file");
		}
	}

	public static String hasEvent(MeetupEvent event) {
		return events.stream().filter(o -> o.first().equals(event.getID())).map(o -> o.second()).findFirst().orElse(""); 
	}
}
