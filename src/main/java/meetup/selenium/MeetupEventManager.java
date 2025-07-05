package meetup.selenium;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import gamebot.ChannelLogger;
import gamebot.Utils;
import misc.RedisConnector;
import reactor.core.publisher.Mono;

public class MeetupEventManager {

	private static String key = "gamebot:MeetupEvents";
	private static HashMap<String, Pair<String, String>> events = new HashMap<>();
	
	public static Mono<Void> init() {
		return RedisConnector.cacheFile(new File("events"), key)
				.flatMap(map -> {
					map.forEach((k, v) -> events.put(k, Pair.of(v.split(" ")[0], v.split(" ")[1])));
					return Mono.empty();
				}).then();
	}
	
	public static Mono<Void> addEvent(String messageId, String eventId, String timeToDelete) {
		Pair<String, String> pair = Pair.of(eventId, timeToDelete);
		events.put(messageId, pair);
		return RedisConnector.cacheEntry(key, Pair.of(messageId, Arrays.asList(eventId, timeToDelete)));
	}
	
	public static Mono<ArrayList<String>> scheduleMessagesForDeletion() {
		return ChannelLogger.logMessageInfo("Scheduling past events for deletion...")
				.then(Mono.fromCallable(() -> {
					ZonedDateTime time = ZonedDateTime.now();
					ArrayList<String> toRemove = events.entrySet().stream()
							.filter(entry -> {
								Pair<String, String> data = entry.getValue();
								ZonedDateTime check = ZonedDateTime.parse(data.getRight(), Utils.getDateFormatter()).plusHours(16);
								return time.compareTo(check) > 0; 
							})
							.map(o -> o.getKey())
							.collect(Collectors.toCollection(ArrayList::new));
					events.entrySet().removeIf(o -> toRemove.contains(o.getKey()));
					return toRemove;
				})
				.flatMap(toRemove -> ChannelLogger.logMessageInfo("Found "+toRemove.size()+ ", Events list now contains "+events.size())
					.then(RedisConnector.deleteEntry(key, toRemove.toArray(new String[0])))
					.then(Mono.just(toRemove))));
	}
	
	public static String hasEvent(String eventId) {
		return events.entrySet().stream()
				.filter(o -> o.getKey().equals(eventId))
				.map(o -> o.getValue().getLeft()).findFirst().orElse(""); 
	}
}
