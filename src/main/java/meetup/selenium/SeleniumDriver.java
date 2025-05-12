package meetup.selenium;

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

	private final String baseXPath = "//div[starts-with(@id, 'e-')]/a";
	private final String eventName = "//main//h1";
	private final String eventDate = "//div[contains(@class,'pl-4')]/time";
	private final String attendeeText = "//div[@id='attendees']/div/h2";

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
			ClickElement("//button[contains(text(), 'Log in')]");
			ClickElement("//button[@id='onetrust-accept-btn-handler']");
			unlock();
			return true;
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Unable to login to Meetup.", e);
			unlock();
			return false;
		}
	}
	
	public ArrayList<String> getEventIds(){
		ArrayList<String> ids = new ArrayList<>();
		lock();
		try {
			webDriver.get(meetupUrl);
			if (!doesElementExist(baseXPath, "Checking to see if there are any events"))
				return ids;
			ArrayList<WebElement> cards = Elements(baseXPath);
			for (WebElement card : cards) {
				ChannelLogger.logMessageInfo("Adding card to list...");
				String url = getEventUrl(card);
				ids.add(extractIdFromMeetupUrl(url));
			}
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to compile ids", e);
		} finally {
			unlock();
		}
		return ids;
	}

	public long checkCode(String code) {
		lock();
		String messagesUrl = ("https://www.meetup.com/messages");
		long meetupId = 0L;
		try {
			webDriver.get(messagesUrl);
			Element("//p[text()='"+code+"']").click();
			Thread.sleep(3000);
			Element("//div[contains(@class,'styles_messages')]//button").click();
			String profileUrl = Element("//a[text()='View Profile']").getAttribute("href");
			meetupId = new Long(extractIdFromMeetupUrl(profileUrl)).longValue();
		} catch (Exception e) {
			ChannelLogger.logMessageError("Error occurred during verification due to "+e.getStackTrace()[0]);
		} finally {
			unlock();
		}
		return meetupId;
	}

	public ArrayList<Pair<String, String>> collateAttendees(String eventId) {
		String attendeeList = "//main//div[@class='mt-6 ']/div/div[@class='flex']";
		lock();
		try {
			ArrayList<Pair<String, String>> attendeePair = new ArrayList<>();
			webDriver.get(meetupUrl + eventId + "/attendees/");
			int attendees = Elements(attendeeList).size();
			for (int i = 1; i <= attendees; i++) {
				String name = TextOf(attendeeList + "[" + i + "]//preceding-sibling::span");
				String link = extractIdFromMeetupUrl(getAttendeeId(attendeeList + "[" + i + "]//button"));
				log.info("Extracted: " + name + ", " + link);
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
			if (!doesElementExist(baseXPath, "Checking to see if there are any events"))
				return eventData;
			ArrayList<WebElement> cards = Elements(baseXPath);
			ArrayList<String> urls = new ArrayList<>();
			for (WebElement card : cards) {
				ChannelLogger.logMessageInfo("Adding card to list...");
				urls.add(getEventUrl(card));
			}
			for (String url : urls) {
				log.info("Resolving URL " + url);
				ChannelLogger.logMessageInfo("Resolving URL " + url.replace("https://www.", ""));
				webDriver.get(url);
				eventData.add(compileEvent(url));
				webDriver.get(meetupUrl);
				ChannelLogger.logMessageInfo("Successfully compiled event");
			}
			return eventData;
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to compile full event data.", e);
			return eventData;
		} finally {
			unlock();
		}
	}

	private MeetupEvent compileEvent(String eventUrl) {
		MeetupEvent event = new MeetupEvent();
		try {
			String eventId = extractIdFromMeetupUrl(eventUrl);
			event.addId(eventId).addUrl(eventUrl).addName(TextOf(eventName)).addDate(TextOf(eventDate))
					.addCurrentAttendees(TextOf(attendeeText));
		} catch (Exception e) {
			ChannelLogger.logHighPriorityMessage("Failed to compile event.", e);
		}
		return event;
	}

	private boolean doesElementExist(String xpath, String reason) {
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
			return true;
		} catch (Exception e) {
			ChannelLogger.logMessageError(reason + ": Could not find element");
			return false;
		}
	}

	private String getAttendeeId(String link) {
		// Open new tab and enable full list
		ClickElement("//div[@data-testid='attendees-tab-container']/button[2]");
		ClickElement("//div[@data-testid='attendees-tab-container']/button[1]");		
		try {
			ClickElement(link);
			// Wait for page to load
			Thread.sleep(200);

			// Get tabs and switch between them
			ArrayList<String> tabs = new ArrayList<>(webDriver.getWindowHandles());
			webDriver.switchTo().window(tabs.get(1));
			String id = extractIdFromMeetupUrl(webDriver.getCurrentUrl());
			webDriver.close();
			webDriver.switchTo().window(tabs.get(0));
			Thread.sleep(100);
			return id;
		} catch (Exception e) {
			return "Attendee";
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

	private void ClickElement(String xpath) {
		int attempts = 0;
		while (attempts < 2) {
			try {
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath))).click();
				break;
			} catch (StaleElementReferenceException e) {
				// Do it again
			}
			attempts++;
		}
	}

}
