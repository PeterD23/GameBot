package gamebot;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;

public class Utils {

	public static boolean testingMode = false;
	public static boolean denyAdmins = false;

	public static Color randomColor() {
		Random random = new Random();
		int r = random.nextInt(200) + 55;
		int g = random.nextInt(200) + 55;
		int b = random.nextInt(200) + 55;
		return Color.of(r, g, b);
	}
	
	@SafeVarargs
	public static <T> ArrayList<T> combineDistinct(List<T>... lists){
		List<T> list = Stream.of(lists).flatMap(Collection::stream).distinct().collect(Collectors.toList());
		return new ArrayList<>(list);
	}
	
	private static String randomHellos[] = new String[] { "I hope you're having a wonderful day! :D",
			"Do you get to the cloud district often?", "Nice weather we're having!",
			"Are you excited for any upcoming games?", "I am certainly enjoying your company :heart:",
			"You're my number one member here on this Discord!", "Let's see if we can't find those mountain lions." };
	
	public static String getARandomGreeting() {
		Random random = new Random();
		return randomHellos[random.nextInt(randomHellos.length)];
	}

	public static String flipTestMode() {
		testingMode = !testingMode;
		return testingMode ? "Testing mode is now enabled. You can now run a local copy of the bot and have it perform actions." :
			"Testing mode is now disabled. Be sure to not be running the bot locally to avoid action duplication.";
	}

	public static boolean isTestingMode() {
		return testingMode;
	}

	public static String flipAdminDenial() {
		denyAdmins = !denyAdmins;
		return denyAdmins ? "Certain commands now denied to admins." :
			"All command re-enabled for admins.";
	}

	public static boolean adminsDenied() {
		return denyAdmins;
	}
	
	public static Pair<String, String> listToPair(List<String> list){
		if(list.size() != 2) {
			throw new RuntimeException("List is not of size 2");
		}
		return Pair.of(list.get(0), list.get(1));
	}
	
	public static String listToSSVString(List<String> list) {
		return String.join(" ", list);
	}
	
	public static String getContentFromComponents(List<TopLevelMessageComponent> components) {
		String data = "";
		for(TopLevelMessageComponent component : components) {
			Possible<String> maybe = component.getData().content();
			if(!maybe.isAbsent()) {
				data += maybe.get();
			}
		}
		return data;
	}
	
	public static int recursiveLength(ComponentData data) {
		if(data.components().isAbsent())
			return data.content().isAbsent() ? 0 : data.content().get().length();
		return data.components().get().stream().mapToInt(d -> recursiveLength(d)).sum();
	}
	
	public static String tabPad(int size) {
		return StringUtils.rightPad("\t", size);
	}
	
	public static DateTimeFormatter getDateFormatter() {
		DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
		builder.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0);
		return builder.toFormatter();
	}
	
	public static DateTimeFormatter getHumanReadableDateFormatter() {
		return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT);
	}
}
