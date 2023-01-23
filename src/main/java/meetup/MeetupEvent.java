package meetup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MeetupEvent {

	private String id;
	private String url;
	private String name;
	private String startDateTime;
	private String attendees;

	public MeetupEvent addId(String id) {
		this.id = id;
		return this;
	}

	public MeetupEvent addUrl(String url) {
		this.url = url;
		return this;
	}

	public MeetupEvent addName(String name) {
		this.name = name;
		return this;
	}

	public MeetupEvent addDate(String start) {
		String cleaned = start.replace("\n", " at ").replaceAll(" to.{1,}", ""); // I fucking hate you Meetup
		this.startDateTime = cleaned;
		return this;
	}

	public MeetupEvent addCurrentAttendees(String attendees) {
		this.attendees = attendees;
		return this;
	}

	@Override
	public String toString() {
		return id != null
				? (name + "\n" + startDateTime + "\n" + attendees + "\n You can sign up from this link: " + url)
				: "err";
	}

	public String getID() {
		return id;
	}

	public String getDate() {
		/**
		 * Split Date: #0 = Day (3 char) #1 = Month (3 char) #2 = Day of Month #3 = Year
		 * #5 = Time #6 = AM/PM
		 */
		String parseableDate;
		try {
			String[] splitDate = startDateTime.split(", | ");
			parseableDate = splitDate[0].substring(0, 3) + ", " + splitDate[2] + " "
					+ splitDate[1].substring(0, 3) + " " + splitDate[3] + " " + convertAmPm(splitDate[5], splitDate[6])
					+ " GMT";
			return LocalDateTime.parse(parseableDate, DateTimeFormatter.RFC_1123_DATE_TIME).toString();
		} catch (Exception e) {
			return "err";
		}	
	}

	private String convertAmPm(String time, String amPm) {
		String[] split = time.split(":");
		int hour = Integer.parseInt(split[0]) + (amPm.equals("PM") ? 12 : 0);
		return String.join(":", String.valueOf(hour), split[1]); // Fuck you
	}
}
