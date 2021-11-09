package onlineevent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
