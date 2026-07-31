package meetup.api;
import com.fasterxml.jackson.annotation.JsonGetter;

public class EventGraphRequestDTO {

	private String query;
	private Variables variables;
	
	public EventGraphRequestDTO(String query, String eventId) {
		this.query = query;
		this.variables = new Variables(eventId);
	}
	
	public EventGraphRequestDTO(String query) {
		this.query = query;
	}
	
	@JsonGetter("query")
	public String getQuery() {
		return query;
	}
	
	@JsonGetter("variables")
	public Variables getVariables() {
		return variables;
	}
	
	private class Variables {
		
		public Variables(String eventId) {
			this.eventId = eventId;
		}
		
		private String eventId;
		
		@JsonGetter("eventId")
		public String getEventId() {
			return eventId;
		}
		
	}
	
}
