package meetup.selenium;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import reactor.util.Logger;
import reactor.util.Loggers;

public class MeetupLinker {

	// Hey Java Generics, can you fucking please support primitive types so I don't have to keep boxing and unboxing shit
	private static Logger log = Loggers.getLogger("logger");
	private static HashMap<Long, String> queuedUsers = new HashMap<>();
	private static HashMap<Long, Long> verified = new HashMap<>();
	
	public static void readVerified() {
		verified.clear();
		try {
			List<String> lines = FileUtils.readLines(new File("verified"), Charset.defaultCharset());
			for (String line : lines) {
				String[] data = line.split(" ");
				verified.put(new Long(data[0]), new Long(data[1]));
			}
			log.info("Successfully read in "+lines.size()+ " lines for verified users");
		} catch (IOException e) {
			log.warn("Failed to read file for verified users");
		}
	}
	
	private static void saveVerified() {
		ArrayList<String> lines = new ArrayList<>();
		for (Map.Entry<Long, Long> entry : verified.entrySet()) {
			lines.add(entry.getKey() + " " + entry.getValue());
		}
		try {
			FileUtils.writeLines(new File("verified"), lines);
			log.info("Successfully saved "+lines.size()+ " lines for verified users");
		} catch (IOException e) {
			log.warn("Failed to save verified users to file");
		}
	}
	
	public static HashMap<Long, Long> getVerifiedUsers(){
		return verified;
	}
	
	public static boolean queueUser(long userId, String code) {
		// The white zone is for the boxing and unboxing of longs only
		// Listen, Betty, dont start up with your white zone shit again 
		Long userIdLong = new Long(userId);
		if(verified.get(userIdLong) != null) {
			return true;
		}
		log.info("Queuing Member ID "+ userId + " with a temp verification code of "+code);
		queuedUsers.put(userIdLong, code);
		return false;
	}
	
	public static long getMeetupUser(long id) {
		return verified.getOrDefault(id, 0L);
	}
	
	public static long getUserByMeetupId(long id) {
		log.info("Searching for id "+id+"... in verified set of "+verified.size());
		for (Map.Entry<Long, Long> entry : verified.entrySet()) {
		    if(id == entry.getValue().longValue()) {
		    	log.info("Found!");
		    	return entry.getKey().longValue();
		    }
		}
		log.info("Not found, returning zero");
		return 0L;
	}
	
	public static boolean isQueued(long userId) {
		Long userIdLong = new Long(userId);
		return queuedUsers.get(userIdLong) != null;
	}
	
	public static String getUsersCode(long userId) {
		Long userIdLong = new Long(userId);
		return queuedUsers.get(userIdLong);
	}
	
	public static void linkUserToMeetup(long userId, long meetupId) {
		Long userIdLong = new Long(userId);
		verified.put(userIdLong, meetupId);
		queuedUsers.remove(userIdLong);
		saveVerified();
	}
}
