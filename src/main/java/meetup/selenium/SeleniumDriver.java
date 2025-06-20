package meetup.selenium;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
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
import reactor.core.publisher.Mono;
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
	}

	private ChromeOptions setHeadlessMode() {
		return new ChromeOptions().addArguments("--headless","--window-size=1920,1080");
	}
	
	public String refreshCookie(String token) {
		try {
			FileUtils.write(new File("cookie"), token, Charset.defaultCharset());
			login();
			return "Mmmmm... Delicious cookie :cookie:";
		} catch (IOException e) {
			ChannelLogger.logMessageError("Couldn't save the cookie :(", e);
			return "I think that cookie was rancid because my system didn't like it :(";
		}
	}

	public Mono<Void> login() {
		// Want to see a magic trick?
		lock();
		try {
			webDriver.get("https://www.meetup.com");
			Thread.sleep(3000);
			// Pass the session cookie in, organically sourced
			webDriver.manage().addCookie(
					new Cookie(
							"__meetup_auth_access_token", 
							Files.readLines(new File("cookie"), Charset.defaultCharset()).get(0)
							));
			webDriver.navigate().refresh();
			// Tada! How to log in to a website without solving a captcha
			Element("//img[@alt='Photo of Bot McBotterson']");
			unlock();
			return ChannelLogger.logMessageInfo("Successfully logged into Meetup :cookie:");
		} catch (Exception e) {
			unlock();
			return ChannelLogger.logHighPriorityMessage("Unable to login to Meetup. Cookie may be expired and need to be refreshed.", e);
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
			ChannelLogger.logMessageError("Error occurred during verification due to ",e);
		} finally {
			unlock();
		}
		return meetupId;
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
			ChannelLogger.logMessageError(reason + ": Could not find element", e);
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

	public void rsvpUser(String eventId, String[] data) {
		webDriver.get(meetupUrl+eventId+"/attendees");
		Element("//input[@data-testid='attendee-search']").sendKeys(data[0]);
		ClickElement("//img[@src='"+data[1]+"']/ancestor::div[6]//button[@aria-haspopup='menu']");
		ClickElement("//button[text()='Move to \"Going\"']");
	}

}
