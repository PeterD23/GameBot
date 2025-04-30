package meetup.api;
import com.fasterxml.jackson.annotation.JsonGetter;

public class GraphRequestDTO {

	private String query;
	private Variables variables;
	
	public GraphRequestDTO(String query, String eventId) {
		this.query = query;
		this.variables = new Variables(eventId);
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
