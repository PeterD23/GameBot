package gamebot;

import discord4j.rest.util.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
	
	private static String randomHellos[] = new String[] {
		"I hope you're having a wonderful day! :D",
		"Do you get to the cloud district often?",
		"Nice weather we're having!",
		"Are you excited for any upcoming games?",
		"I am certainly enjoying your company :heart:",
		"You're my number one member here on this Discord!"
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
	
	public static void flipAdminDenial() {
		denyAdmins = !denyAdmins;
	}
	
	public static boolean adminsDenied() {
		return denyAdmins;
	}
	
	public static ObjectMapper buildObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		return mapper;
	}
	
	public static String constructMultiLineString(int breakSize, String... strings) {
		StringBuilder sb = new StringBuilder();
		String lineBreak = "\n";
		for(int i = 1; i < breakSize; i++) {
			lineBreak += "\n";
		}
		for(String s : strings) {
			sb.append(s).append(lineBreak);
		}
		return sb.toString();
	}
	
	public static void downloadJPG(String url, String name, int px) {
		char ps = File.separatorChar;
		File filePath = new File(System.getProperty("user.home") + ps + "Pictures" + ps + name+".jpg");
		System.out.println(filePath.getPath());
		try {
			FileUtils.copyURLToFile(new URL(url), filePath, 3000, 5000);
			BufferedImage image = ImageIO.read(filePath);
			ImageIO.write(Scalr.resize(image, px), "jpg", filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SafeVarargs
	public static <T> T randomOf(T... types) {
		Random random = new Random();
		return types[random.nextInt(types.length)];
	}
	
}
