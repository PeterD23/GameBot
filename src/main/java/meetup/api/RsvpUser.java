package meetup.api;

import meetup.selenium.MeetupLinker;

public class RsvpUser {

	public enum RsvpStatus {
		YES, NO, WAITLIST
	}
	
	private String name;
	private String id;
	private String mention;
	private RsvpStatus status;

	public RsvpUser(String name, String id, RsvpStatus status) {
		this.name = name.split(" ")[0];
		this.id = id;
		this.status = status;
		
		long userId = MeetupLinker.getUserByMeetupId(Long.parseLong(id));
		mention = userId != 0L ? ": <@"+userId+">" : "";
	}
	
	public boolean isVerified() {
		return mention.length() > 0;
	}
	
	public RsvpStatus getStatus() {
		return status;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public String toString(){
		return name + mention;
	}
}
