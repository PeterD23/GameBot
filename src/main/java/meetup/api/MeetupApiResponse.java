package meetup.api;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.MediaGallery;
import discord4j.core.object.component.MediaGalleryItem;
import discord4j.core.object.component.Section;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.Separator.SpacingSize;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.component.UnfurledMediaItem;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import gamebot.Utils;
import meetup.api.RsvpUser.RsvpStatus;

public class MeetupApiResponse {

	private String id;
	private String title;
	private String description;
	private String dateTime;
	private String eventUrl;
	private String eventPhotoUrl;

	private String maxTickets;
	private String yesCount;
	private String waitlistCount;
	private String noCount;
	
	private String location;
	private String latLong;
	
	private ArrayList<RsvpUser> members;
	
	public MeetupApiResponse(JsonNode root) {
		members = new ArrayList<>();
		
		TreeNode event = root.at("/data/event");
		id = event.at("/id").toString().replaceAll("\"", "");
		title = event.at("/title").toString().replaceAll("\"", "");
		description = event.at("/description").toString().replaceAll("\"", "").replaceAll("\\\\n", "\n");
		dateTime = event.at("/dateTime").toString().replaceAll("\"", "");
		eventUrl = event.at("/eventUrl").toString().replaceAll("\"", "");
		eventPhotoUrl = event.at("/featuredEventPhoto/highResUrl").toString().replaceAll("\"", "");
		maxTickets = event.at("/maxTickets").toString().replaceAll("\"", "");
		
		yesCount = event.at("/rsvps/yesCount").toString().replaceAll("\"", "");
		waitlistCount = event.at("/rsvps/waitlistCount").toString().replaceAll("\"", "");
		noCount = event.at("/rsvps/noCount").toString().replaceAll("\"", "");
		
		TreeNode venue = event.at("/venues").path(0);
		location = (venue.at("/name").toString() + ",\n"+ venue.at("/address").toString() + ",\n"+ venue.at("/city").toString()).replaceAll("\"", "");
		latLong = (venue.at("/lat").toString() + "," + venue.at("/lon").toString()).replaceAll("\"", "");
		
		TreeNode memberTree = event.at("/rsvps/edges");
		for(int i = 0; i < memberTree.size(); i++) {
			String name = memberTree.path(i).at("/node/member/name").toString().replaceAll("\"", "");
			String id = memberTree.path(i).at("/node/member/id").toString().replaceAll("\"", "");
			String status = memberTree.path(i).at("/node/status").toString().replaceAll("\"", "");
			members.add(new RsvpUser(name, id, RsvpStatus.valueOf(status)));
		}
	}

	public boolean canRsvp() {
		int currentRsvps = Integer.parseInt(yesCount);
		int maxRsvps = Integer.parseInt(maxTickets);
		return currentRsvps < maxRsvps; 
	}
	
	public String getDateTime() {
		return dateTime;
	}

	public ArrayList<RsvpUser> getUsers(){
		return members;
	}
	
	private String generatePaddedAttendees(RsvpStatus status, boolean attendeeCategories) {
		// Get Meetup Verified members
		ArrayList<RsvpUser> filtered = new ArrayList<>(members.stream().filter(member -> member.getStatus() == status && member.isVerified()).collect(Collectors.toList()));
		StringBuilder attendees = new StringBuilder(filtered.size() > 0 && attendeeCategories ? "### Hosts and Friendly Faces\n":"");
		for(int i = 0; i < filtered.size(); i++) {
			attendees.append(filtered.get(i) + Utils.tabPad(2)).append(i+1 % 3 == 0 ? "\n":"");
		}
		
		// Get non-Meetup Verified members
		filtered = new ArrayList<>(members.stream().filter(member -> member.getStatus() == status && !member.isVerified()).collect(Collectors.toList()));
		attendees.append(filtered.size() > 0 && attendeeCategories ? "\n### Other Attendees\n":"");
		for(int i = 0; i < filtered.size(); i++) {
			attendees.append(filtered.get(i) + Utils.tabPad(3)).append(i+1 % 6 == 0 ? "\n":"");
		}	
		return attendees.toString();
	}
	
	public ArrayList<TopLevelMessageComponent> build() {
		ArrayList<TopLevelMessageComponent> components = new ArrayList<>();
		String humanDateTime = LocalDateTime.parse(dateTime, Utils.getDateFormatter()).format(Utils.getHumanReadableDateFormatter());
		// Basic Event Details
		components.add(TextDisplay.of("everyone\n# New event just dropped :fire:"));
		components.add(
				Container.of(Color.RED,
						Section.of(
								Button.link(eventUrl, "Meetup URL"),
								TextDisplay.of("# "+title),
								TextDisplay.of(description)
								),
						ActionRow.of(Button.primary("rsvp_"+id, "RSVP ("+yesCount+"/"+maxTickets+")").disabled(!canRsvp()))
						)
				);
		// Location and Date Time
		components.add(
				Container.of(Color.ORANGE,
						Section.of(
								Button.link("https://maps.google.com/?q="+latLong, ReactionEmoji.codepoints("U+1F9ED")),
								TextDisplay.of("# "+location), 
								TextDisplay.of("## "+humanDateTime)
								),
						MediaGallery.of(MediaGalleryItem.of(UnfurledMediaItem.of(eventPhotoUrl)))
						)	
				);
		// Going
		components.add(
				Container.of(Color.YELLOW,
								TextDisplay.of("# :white_check_mark: Going ("+yesCount+"/"+maxTickets+")\n"+generatePaddedAttendees(RsvpStatus.YES, true)), 
								Separator.of(SpacingSize.LARGE),
								TextDisplay.of("# :hourglass: Waitlist ("+waitlistCount+")\n"+generatePaddedAttendees(RsvpStatus.WAITLIST, false)), 
								Separator.of(SpacingSize.LARGE),
								TextDisplay.of("# :x: Not Going ("+noCount+")\n"+generatePaddedAttendees(RsvpStatus.NO, false))
						)
				);	
			
		return components;
	}

}
