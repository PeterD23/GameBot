package meetup.api;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MeetupConfig {

	private String meetupUser;
	private String meetupPass;
	private String clientId;
	private String clientSecret;
	private String refreshToken;
	
	@JsonGetter("meetupUser")
	public String getMeetupUser() {
		return meetupUser;
	}
	
	@JsonGetter("meetupPass")
	public String getMeetupPass() {
		return meetupPass;
	}
	
	@JsonGetter("clientId")
	public String getClientId() {
		return clientId;
	}
	
	@JsonGetter("clientSecret")
	public String getClientSecret() {
		return clientSecret;
	}
	
	@JsonGetter("refreshToken")
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
	
}
