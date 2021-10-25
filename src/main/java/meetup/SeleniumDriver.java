package meetup;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.io.Files;

import io.github.bonigarcia.wdm.WebDriverManager;
import reactor.util.Logger;
import reactor.util.Loggers;

public class SeleniumDriver {

	private static Logger log = Loggers.getLogger("logger");
	private WebDriver webDriver;
	private final String meetupUrl = "https://www.meetup.com/Edinburgh-Local-Video-Gamers/events/";
	private FluentWait<WebDriver> wait;

	private final String baseXPath = "//div[contains(@class,'eventList')]//a[contains(@class,'eventCard--link')]";
	private final String eventName = "//h1[contains(@class,'pageTitle')]";
	private final String eventDate = "//span[contains(@class,'eventTimeDisplay-startDate')]";
	private final String startTime = "//span[contains(@class,'eventTimeDisplay-startDate-time')]/span";
	private final String attendeeText = "//h3[contains(@class,'attendees')]/span";

	private static SeleniumDriver instance;

	public static SeleniumDriver getInstance() {
		if (instance == null) {
			instance = new SeleniumDriver();
			return instance;
		}
		return instance;
	}

	private SeleniumDriver() {
		WebDriverManager.chromedriver().setup();
		webDriver = new ChromeDriver(setHeadlessMode());
		wait = new WebDriverWait(webDriver, 5).ignoring(StaleElementReferenceException.class);
		if (login())
			log.info("Successfully logged in to Meetup");
		else
			log.error("Failed to log into Meetup");
	}

	private ChromeOptions setHeadlessMode() {
		return new ChromeOptions();// ChromeOptions().addArguments("--headless", "--window-size=1920,1080");
	}

	public boolean login() {
		try {
			ArrayList<String> creds = new ArrayList<>(Files.readLines(new File("creds"), Charset.defaultCharset()));
			webDriver.get("https://www.meetup.com");
			Element("//a[contains(@id, 'login-link')]").click();
			Element("//input[contains(@type, 'email')]").sendKeys(creds.get(0));
			Element("//input[contains(@type, 'password')]").sendKeys(creds.get(1));
			Element("//button[contains(@type, 'submit')]").click();
			return true;
		} catch (Exception e) {
			log.error("Unable to login due to " + e.getStackTrace()[0].toString());
			return false;
		}
	}

	public long sendCode(String meetupUrl, String code) {
		long meetupId = new Long(extractIdFromMeetupUrl(meetupUrl));
		try {
			webDriver.get(meetupUrl);
			Element("//a[contains(@id, 'message-link')]").click();
			Thread.sleep(3000);
			Element("//textarea[contains(@class, 'composeBox-textArea')]").click();
			Element("//textarea[contains(@class, 'composeBox-textArea')]").sendKeys(code);
			Element("//button[contains(@class, 'composeBox-sendButton')]").click();
		} catch (Exception e) {
			return 0;
		}
		return meetupId;
	}

	public ArrayList<MeetupEvent> returnEventData() {
		ArrayList<MeetupEvent> eventData = new ArrayList<>();
		webDriver.get(meetupUrl);
		ArrayList<WebElement> cards = Elements(baseXPath);
		ArrayList<String> urls = new ArrayList<>();
		for (WebElement card : cards) {
			urls.add(getEventUrl(card));
		}
		for (String url : urls) {
			webDriver.get(url);
			eventData.add(compileEvent(url));
			webDriver.get(meetupUrl);
		}
		return eventData;
	}

	private MeetupEvent compileEvent(String eventUrl) {
		MeetupEvent event = new MeetupEvent();
		try {
			String eventId = extractIdFromMeetupUrl(eventUrl);
			event.addId(eventId).addUrl(eventUrl).addName(TextOf(eventName))
					.addDate(TextOf(eventDate) + " at " + TextOf(startTime)).addCurrentAttendees(TextOf(attendeeText));
		} catch (Exception e) {
			log.error("Could not parse event data.");
		}
		return event;
	}

	private String extractIdFromMeetupUrl(String url) {
		return url.replaceAll("\\D{0,}", "");
	}

	private String getEventUrl(WebElement element) {
		return element.getAttribute("href");
	}

	private String TextOf(String xpath) {
		return Element(xpath).getText();
	}

	private ArrayList<WebElement> Elements(String xpath) {
		return new ArrayList<>(wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(xpath))));
	}

	private WebElement Element(String xpath) {
		return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
	}

}
