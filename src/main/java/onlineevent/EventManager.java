package onlineevent;

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

public class EventManager {

	private static Logger log = Loggers.getLogger("logger");
	private static ArrayList<OnlineEvent> events = new ArrayList<>();
	
	public static void init() {
		events = new ArrayList<>();
		loadEventData();		
	}
	
	public static ArrayList<OnlineEvent> getEvents() {
		return events;
	}
	
	public static void add(OnlineEvent event) {
		events.add(event);
		saveEventData();
	}
	
	public static ArrayList<Long> scheduleMessagesForDeletion() {
		log.info("Scheduling past online events for deletion...");
		LocalDateTime time = LocalDateTime.now();
		ArrayList<Long> toRemove = events.stream().filter(o -> {
			LocalDateTime check = o.getDateTime().plusMinutes(30);
			return time.compareTo(check) > 0; 
		}).map(o -> o.getMessageId()).collect(Collectors.toCollection(ArrayList::new));
		events.removeIf(o -> toRemove.contains(o.getMessageId()));
		log.info("Found "+toRemove.size()+ ", Online Events list now contains "+events.size());
		saveEventData();
		return toRemove;
	}
	
	public static void saveEventData() {
		ArrayList<String> data = new ArrayList<>();
		for(OnlineEvent event : events) {
			data.add(event.asWritableString());
		}
		try {
			FileUtils.writeLines(new File("onlineEvents"), data);
		} catch (IOException e) {
			log.warn("Failed to read file 'onlineEvents'");
		}
	}
	
	private static void loadEventData() {
		events.clear();
		try {
			List<String> lines = FileUtils.readLines(new File("onlineEvents"), Charset.defaultCharset());
			for (String line : lines) {
				events.add(new OnlineEvent(line));
			}
		} catch (IOException e) {
			log.warn("Failed to read file 'onlineEvents'");
		}
	}
	
}
