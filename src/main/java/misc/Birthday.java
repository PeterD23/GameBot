package misc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import reactor.util.Logger;
import reactor.util.Loggers;

public class Birthday {

	private static Logger log = Loggers.getLogger("logger");
	private static HashMap<Long, String> birthdays = new HashMap<>();
	
	public static void init() {
		birthdays = new HashMap<>();
		readBirthdays();		
	}
	
	public static ArrayList<Long> hasBirthdaysToday(){
		LocalDate today = LocalDate.now();
		ArrayList<Long> list = new ArrayList<>();
		for (Map.Entry<Long, String> entry : birthdays.entrySet()) {
			String todayStr = today.getDayOfMonth()+","+today.getMonthValue();
			if( todayStr.equals(entry.getValue())) {
				list.add(entry.getKey());
			}
		}
		return list;
	}
	
	public static void add(long userId, LocalDate date) {
		birthdays.put(new Long(userId), date.getDayOfMonth()+","+date.getMonthValue());
		saveBirthdays();
	}
	
	public static void readBirthdays() {
		birthdays.clear();
		try {
			List<String> lines = FileUtils.readLines(new File("birthdays"), Charset.defaultCharset());
			for (String line : lines) {
				String[] data = line.split(" ");
				birthdays.put(new Long(data[0]), new String(data[1]));
			}
			log.info("Successfully read in "+lines.size()+ " lines for verified users");
		} catch (IOException e) {
			log.warn("Failed to read file for verified users");
		}
	}
	
	private static void saveBirthdays() {
		ArrayList<String> lines = new ArrayList<>();
		for (Map.Entry<Long, String> entry : birthdays.entrySet()) {
			lines.add(entry.getKey() + " " + entry.getValue());
		}
		try {
			FileUtils.writeLines(new File("birthdays"), lines);
			log.info("Successfully saved "+lines.size()+ " lines for birthdays");
		} catch (IOException e) {
			log.warn("Failed to save birthdays to file");
		}
	}
	
}
