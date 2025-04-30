package meetup.api;
import java.util.ArrayList;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

public class MeetupApiResponse {

	private String title;
	private String description;
	private String dateTime;
	private String eventUrl;

	private ArrayList<RsvpUser> users;
	
	public MeetupApiResponse(JsonNode root) {
		users = new ArrayList<>();
		
		TreeNode event = root.at("/data/event");
		title = event.at("/title").toString().replaceAll("\"", "");
		description = event.at("/description").toString().replaceAll("\"", "");
		dateTime = event.at("/dateTime").toString().replaceAll("\"", "");
		eventUrl = event.at("/eventUrl").toString().replaceAll("\"", "");
		
		TreeNode userTree = event.at("/tickets/edges");
		for(int i = 0; i < userTree.size(); i++) {
			String name = userTree.path(i).at("/node/user/name").toString().replaceAll("\"", "");
			String id = userTree.path(i).at("/node/user/id").toString().replaceAll("\"", "");
			users.add(new RsvpUser(name, id));
		}
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}
	
	public String getDateTime() {
		return dateTime;
	}
	
	public String getEventUrl() {
		return eventUrl;
	}
	
	public ArrayList<RsvpUser> getUsers(){
		return users;
	}
	
	@Override
	public String toString() {
		return (title + "\n" + dateTime.replace("T", " ") + "\nAttendees (" + users.size() + ")\n You can sign up from this link: " + eventUrl);
	}

}
