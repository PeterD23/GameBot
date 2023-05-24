package onlineevent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;

import reactor.util.Logger;
import reactor.util.Loggers;

public class OnlineEvent {

	private static Logger log = Loggers.getLogger("logger");
	private long messageId;

	private String createUser;
	private String title;
	private String description;

	private LocalDateTime dateTime;

	private boolean halfHourReminder = false;
	
	private ArrayList<String> attendees = new ArrayList<>();

	public OnlineEvent(String createUser, String data) {
		this.createUser = createUser;
		String[] splitData = data.split("\\|");
		if(splitData.length != 3) {
			log.error("Event did not meet argument criteria. Please try again.");
			return;
		}
		title = splitData[0].replace("!event", "").trim();
		description = splitData[1].trim();
		if (!formatString(splitData[2].trim())) {
			log.error("Error parsing date for event. Please try again.");
		}
	}

	public OnlineEvent(String data) {
		String[] splitData = data.split("\\|");
		messageId = Long.parseLong(splitData[0]);
		createUser = splitData[1];
		title = splitData[2];
		description = splitData[3].replaceAll("$n", "\n");
		dateTime = LocalDateTime.parse(splitData[4]);
		if (splitData.length > 5)
			attendees = new ArrayList<>(Arrays.asList(splitData[5].split(",")));
	}
	
	public boolean checkHalfHourReminder() {
		return halfHourReminder;
	}
	
	public void triggerHalfHourReminder() {
		halfHourReminder = true;
	}
	
	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public long getMessageId() {
		return messageId;
	}
	
	public Long getMessageIdLong() {
		return new Long(messageId);
	}

	public void addMessageId(long messageId) {
		this.messageId = messageId;
	}

	public boolean isValidEvent() {
		return dateTime != null;
	}

	public String toString() {
		return "**" + title + "**\nCreated by " + createUser + "\n\n" + description + "\n\nEvent is at "
				+ dateTime.toString() + "\nAttendees: " + printAttendeesList();
	}

	public String asWritableString() {
		return messageId + "|" + createUser + "|" + title + "|" + description.replaceAll("\n", "$n") + "|" + dateTime.toString() + "|"
				+ String.join(",", attendees);
	}

	public void addAttendee(String attendee) {
		attendees.add(attendee);
	}

	public void removeAttendee(String attendee) {
		attendees.remove(attendee);
	}

	private String printAttendeesList() {
		String list = "";
		for (String attendee : attendees) {
			list += "\n" + attendee;
		}
		return list;
	}
	
	public ArrayList<Long> getAttendeesToDM(){
		ArrayList<Long> dmables = new ArrayList<>();
		for(String attendee : attendees) {
			dmables.add(new Long(attendee.replaceAll("[\\<@\\>]","")));
		}
		return dmables;
	}

	private boolean formatString(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		try {
			dateTime = LocalDateTime.parse(date, formatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
