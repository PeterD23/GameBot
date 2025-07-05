package meetup.selenium;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import gamebot.ChannelLogger;
import misc.RedisConnector;
import misc.Utils;
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
	
	public static Mono<Void> addEvent(String eventId, String messageId, String timeToDelete) {
		Pair<String, String> pair = Pair.of(eventId, timeToDelete);
		events.put(eventId, pair);
		return RedisConnector.cacheEntry(key, Pair.of(eventId, Arrays.asList(messageId, timeToDelete)));
	}
	
	public static Mono<ArrayList<String>> scheduleMessagesForDeletion() {
		return ChannelLogger.logMessageInfo("Scheduling past events for deletion...")
				.then(Mono.fromCallable(() -> {
					ZonedDateTime time = ZonedDateTime.now();
					ArrayList<Pair<String, String>> toRemove = events.entrySet().stream()
							.filter(entry -> {
								Pair<String, String> data = entry.getValue();
								ZonedDateTime check = ZonedDateTime.parse(data.getRight(), Utils.getDateFormatter()).plusHours(16);
								return time.compareTo(check) > 0; 
							})
							.map(o -> Pair.of(o.getKey(), o.getValue().getLeft()))
							.collect(Collectors.toCollection(ArrayList::new));
					events.entrySet().removeIf(o -> toRemove.contains(Pair.of(o.getKey(), o.getValue().getLeft())));
					return toRemove;
				})
				.flatMap(toRemove -> ChannelLogger.logMessageInfo("Found "+toRemove.size()+ ", Events list now contains "+events.size())
					.then(RedisConnector.deleteEntry(key, eventIdList(toRemove).toArray(new String[0])))
					.then(Mono.just(discordMessageIdList(toRemove)))));
	}
	
	private static List<String> eventIdList(ArrayList<Pair<String, String>> pairs){
		return pairs.stream().map(pair -> pair.getLeft()).collect(Collectors.toList());
	}
	
	private static ArrayList<String> discordMessageIdList(ArrayList<Pair<String, String>> pairs){
		return pairs.stream().map(pair -> pair.getRight()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	
	public static String hasEvent(String eventId) {
		return events.entrySet().stream()
				.filter(o -> o.getKey().equals(eventId))
				.map(o -> o.getValue().getLeft()).findFirst().orElse(""); 
	}
}
