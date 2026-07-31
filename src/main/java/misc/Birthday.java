package misc;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

public class Birthday {
	
	private static String key = "gamebot:Birthdays";
	private static HashMap<String, String> birthdays = new HashMap<>();
	
	public static Mono<Void> readBirthdays() {
		return RedisConnector.cacheFile(new File("birthdays"), key)
				.flatMap(map -> {
					birthdays = map;
					return Mono.empty();
				}).then();
	}
	
	public static ArrayList<String> hasBirthdaysToday(){
		LocalDate today = LocalDate.now();
		ArrayList<String> list = new ArrayList<>();
		for (Map.Entry<String, String> entry : birthdays.entrySet()) {
			String todayStr = today.getDayOfMonth()+","+today.getMonthValue();
			if( todayStr.equals(entry.getValue())) {
				list.add(entry.getKey());
			}
		}
		return list;
	}
	
	public static Mono<Void> add(String userId, LocalDate date) {
		String dateFormat = date.getDayOfMonth()+","+date.getMonthValue();
		birthdays.put(userId, dateFormat);
		return RedisConnector.cacheEntry(key, userId, dateFormat);
	}
	
}
