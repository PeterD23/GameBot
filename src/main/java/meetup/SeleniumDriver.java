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

import gamebot.ChannelLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import reactor.util.Logger;
import reactor.util.Loggers;

public class SeleniumDriver {

	private boolean lock = false;

	private static Logger log = Loggers.getLogger("logger");
	private WebDriver webDriver;
	private final String meetupUrl = "https://www.meetup.com/Edinburgh-Local-Video-Gamers/events/";
	private FluentWait<WebDriver> wait;

	private final String baseXPath = "//div[contains(@class,'eventList')]//a[contains(@class,'eventCard--link')]";
	private final String eventName = "//main//h1";
	private final String eventDate = "//div[contains(@class,'pl-4')]/time";
	private final String attendeeText = "//a[text()='See all']/../h2";

	private static SeleniumDriver instance;

	public static SeleniumDriver getInstance() {
		if (instance == null) {
			instance = new SeleniumDriver();
			return instance;
		}
		return instance;
	}

	private void lock() {
		lock = true;
		log.info("Driver is now locked");
	}

	public void unlock() {
		lock = false;
		log.info("Driver is now unlocked");
	}

	public synchronized boolean isLocked() {
		return lock == true;
	}

	private SeleniumDriver() {
		WebDriverManager.chromedriver().setup();
		webDriver = new ChromeDriver(setHeadlessMode());
		wait = new WebDriverWait(webDriver, 15).ignoring(StaleElementReferenceException.class);
		if (login())
			log.info("Successfully logged in to Meetup");
		else
			log.error("Failed to log into Meetup");
	}

	private ChromeOptions setHeadlessMode() {
		return new ChromeOptions().addArguments("--headless", "--window-size=1920,1080");
	}

	public boolean login() {
		lock();
		try {
			ArrayList<String> creds = new ArrayList<>(Files.readLines(new File("creds"), Charset.defaultCharset()));
			webDriver.get("https://www.meetup.com");
			Element("//a[contains(@id, 'login-link')]").click();
			Element("//input[contains(@type, 'email')]").sendKeys(creds.get(0));
			Element("//input[contains(@type, 'password')]").sendKeys(creds.get(1));
			Element("//button[contains(@type, 'submit')]").click();
			unlock();
			return true;
		} catch (Exception e) {
			log.error("Unable to login due to " + e.getStackTrace()[0].toString());
			unlock();
			return false;
		}
	}

	public long sendCode(String meetupUrl, String code) {
		lock();
		long meetupId = new Long(extractIdFromMeetupUrl(meetupUrl)).longValue();
		try {
			webDriver.get(meetupUrl);
			Element("//a[contains(@id, 'message-link')]").click();
			Thread.sleep(3000);
			Element("//textarea[contains(@class, 'composeBox-textArea')]").click();
			Element("//textarea[contains(@class, 'composeBox-textArea')]").sendKeys(code);
			Element("//button[contains(@class, 'composeBox-sendButton')]").click();
		} catch (Exception e) {
			return 0;
		} finally {
			unlock();
		}
		return meetupId;
	}

	public ArrayList<Pair<String, String>> collateAttendees(String eventId) {
		lock();
		try {
			ArrayList<Pair<String, String>> attendeePair = new ArrayList<>();
			webDriver.get(meetupUrl + eventId + "/attendees/");
			ArrayList<WebElement> attendees = Elements("//ul[contains(@class, 'list')]/li");
			for (int i = 1; i <= attendees.size(); i++) {
				String name = TextOf("(//ul[contains(@class, 'list')]/li)[" + i + "]//h4").split("\\s")[0];
				String link = extractIdFromMeetupUrl(
						getEventUrl(Element(("(//ul[contains(@class, 'list')]/li)[" + i + "]//a"))));
				log.info("Found " + i + ": " + name + ", " + link);
				attendeePair.add(new Pair<>(name, link));
			}
			return attendeePair;
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to collate attendee list.", e);
			return new ArrayList<>();
		} finally {
			unlock();
		}
	}

	public ArrayList<MeetupEvent> returnEventData() {
		lock();
		ArrayList<MeetupEvent> eventData = new ArrayList<>();
		try {		
			webDriver.get(meetupUrl);
			if(!doesElementExist(baseXPath, "Checking to see if there are any events"))
				return eventData;
			ArrayList<WebElement> cards = Elements(baseXPath);
			ArrayList<String> urls = new ArrayList<>();
			for (WebElement card : cards) {
				ChannelLogger.logMessage("Adding card to list...");
				urls.add(getEventUrl(card));
			}
			for (String url : urls) {
				log.info("Resolving URL " + url);
				ChannelLogger.logMessage("Resolving URL " + url.replace("https://www", ""));
				webDriver.get(url);
				eventData.add(compileEvent(url));
				webDriver.get(meetupUrl);
				ChannelLogger.logMessage("Successfully compiled event");
			}
			return eventData;
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to compile full event data.",e);
			return eventData;
		} finally {
			unlock();
		}
	}

	private MeetupEvent compileEvent(String eventUrl) {
		MeetupEvent event = new MeetupEvent();
		try {
			String eventId = extractIdFromMeetupUrl(eventUrl);
			event.addId(eventId).addUrl(eventUrl).addName(TextOf(eventName))
					.addDate(TextOf(eventDate)).addCurrentAttendees(TextOf(attendeeText));
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to compile full event data.",e);
		}
		return event;
	}
	
	private boolean doesElementExist(String xpath, String reason) {
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
			return true;
		} catch (Exception e) {
			ChannelLogger.logMessage(reason + ": Could not find element");
			return false;
		}
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
