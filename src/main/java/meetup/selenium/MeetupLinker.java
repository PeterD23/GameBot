package meetup.selenium;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;

import gamebot.ChannelLogger;
import misc.RedisConnector;
import reactor.core.publisher.Mono;

public class MeetupLinker {

	// Hey Java Generics, can you fucking please support primitive types so I don't have to keep boxing and unboxing shit
	private static String key = "gamebot:MeetupVerifiedMembers";
	private static HashMap<String, String> queuedUsers = new HashMap<>();
	private static HashMap<String, String> verified = new HashMap<>();
	
	public static Mono<Void> readVerified() {
		verified.clear();
		return RedisConnector.cacheFile(new File("verified"), key).flatMap(map -> {
			verified = map;
			return Mono.empty();
		}).then();
	}
	
	public static boolean queueUser(String userId, String code) {
		// The white zone is for the boxing and unboxing of longs only
		// Listen, Betty, dont start up with your white zone shit again 
		if(verified.get(userId) != null) {
			return true;
		}
		ChannelLogger.logMessageInfo("Queuing Member ID "+ userId + " with a temp verification code of "+code);
		queuedUsers.put(userId, code);
		return false;
	}
	
	public static String getMeetupUser(String id) {
		return verified.getOrDefault(id, "");
	}
	
	public static Optional<String> getUserByMeetupId(String id) {
		return verified.entrySet().stream()
				.filter(entry -> id.equals(entry.getValue()))
				.map(entry -> entry.getValue())
				.findFirst();
	}
	
	public static boolean isQueued(String userId) {
		return queuedUsers.get(userId) != null;
	}
	
	public static String getUsersCode(String userId) {
		return queuedUsers.get(userId);
	}
	
	public static Mono<Void> linkUserToMeetup(String userId, String meetupId) {
		verified.put(userId, meetupId);
		queuedUsers.remove(userId);
		return RedisConnector.cacheEntry(key, userId, meetupId);
	}
}
