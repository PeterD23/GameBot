package meetup.selenium;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import gamebot.ChannelLogger;
import gamebot.Utils;
import reactor.core.publisher.Mono;

public class MeetupEventManager {

	private static ArrayList<Tuple<String, String, String>> events;
	
	public static Mono<Void> init() {
		return Mono.fromRunnable(() -> {
			events = new ArrayList<>();
			readEventData();
		});
	}
	
	public static void addEvent(String eventId, String messageId, String timeToDelete) {
		events.add(new Tuple<>(eventId, messageId, timeToDelete));
		saveEventData();
	}
	
	public static ArrayList<String> scheduleMessagesForDeletion() {
		ChannelLogger.logMessageInfo("Scheduling past events for deletion...");
		ZonedDateTime time = ZonedDateTime.now();
		ArrayList<String> toRemove = events.stream().filter(o -> {
			ZonedDateTime check = ZonedDateTime.parse(o.third(), Utils.getDateFormatter()).plusHours(16);
			return time.compareTo(check) > 0; 
		}).map(o -> o.second()).collect(Collectors.toCollection(ArrayList::new));
		events.removeIf(o -> toRemove.contains(o.second()));
		ChannelLogger.logMessageInfo("Found "+toRemove.size()+ ", Events list now contains "+events.size());
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
			ChannelLogger.logMessageError("Failed to read file ./events", e);
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
			ChannelLogger.logMessageError("Failed to save events to file ./events", e);
		}
	}
	
	public static String hasEvent(String eventId) {
		return events.stream().filter(o -> o.first().equals(eventId)).map(o -> o.second()).findFirst().orElse(""); 
	}

	public static String hasEvent(MeetupEvent event) {
		return events.stream().filter(o -> o.first().equals(event.getID())).map(o -> o.second()).findFirst().orElse(""); 
	}
}
