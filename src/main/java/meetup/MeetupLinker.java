package meetup;

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

	private static Logger log = Loggers.getLogger("logger");
	private static HashMap<Long, String> queuedUsers = new HashMap<>();
	private static HashMap<Long, Long> tempLinked = new HashMap<>();
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
	
	public static boolean queueUser(Long userId, String code) {
		if(queuedUsers.get(userId) != null || verified.get(userId) != null) {
			return true;
		}
		log.info("Queuing Member ID "+ userId + " with a temp verification code of "+code);
		queuedUsers.put(userId, code);
		return false;
	}
	
	public static Long getUserByMeetupId(Long id) {
		log.info("Searching for id "+id+"... in verified set of "+verified.size());
		for (Map.Entry<Long, Long> entry : verified.entrySet()) {
		    if(id.longValue() == entry.getValue().longValue()) {
		    	log.info("Found!");
		    	return entry.getKey();
		    }
		}
		log.info("Not found, returning zero");
		return 0L;
	}
	
	public static boolean isQueued(Long userId) {
		return queuedUsers.get(userId) != null;
	}
	
	public static String getUsersCode(Long userId) {
		return queuedUsers.get(userId);
	}
	
	public static void addMeetupId(Long userId, Long meetupId) {
		tempLinked.put(userId, meetupId);
	}
	
	public static void verifyUser(Long userId) {
		verified.put(userId, tempLinked.get(userId));
		tempLinked.remove(userId);
		queuedUsers.remove(userId);
		saveVerified();
	}
}
