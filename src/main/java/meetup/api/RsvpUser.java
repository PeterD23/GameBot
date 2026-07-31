package meetup.api;

import java.util.Optional;

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
		
		Optional<String> userId = MeetupLinker.getUserByMeetupId(id);
		mention = userId.isPresent() ? ": <@"+userId.get()+">" : "";
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
