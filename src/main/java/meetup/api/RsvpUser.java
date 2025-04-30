package meetup.api;

public class RsvpUser {

	private String name;
	private String id;

	public RsvpUser(String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}

}
