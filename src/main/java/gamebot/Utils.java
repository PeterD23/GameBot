package gamebot;

import java.awt.Color;
import java.util.Random;

public class Utils {

	public static boolean testingMode = false;
	
	public static Color randomColor() {
		Random random = new Random();
		int r = random.nextInt(200) + 55;
		int g = random.nextInt(200) + 55;
		int b = random.nextInt(200) + 55;
		return new Color(r, g, b);
	}
	
	private static String randomHellos[] = new String[] {
		"I hope you're having a wonderful day! :D",
		"Do you get to the cloud district often?",
		"Nice weather we're having!",
		"Are you excited for any upcoming games?"
	};
	
	public static String getARandomGreeting() {
		Random random = new Random();
		return randomHellos[random.nextInt(randomHellos.length)];
	}
	
	public static void flipTestMode() {
		testingMode = !testingMode;
	}
	
	public static boolean isTestingMode() {
		return testingMode;
	}
	
}
