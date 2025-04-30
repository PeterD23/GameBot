package meetup.api;
import com.fasterxml.jackson.annotation.JsonGetter;

public class JwtDTO {

	private String accessToken;
	private String refreshToken;
	private int expiryTime;
	private String tokenType;
	
	@JsonGetter("access_token")
	public String getAccessToken() {
		return accessToken;
	}
	
	@JsonGetter("refresh_token")
	public String getRefreshToken() {
		return refreshToken;
	}
	
	@JsonGetter("expires_in")
	public int getExpiryTime() {
		return expiryTime;
	}
	
	@JsonGetter("token_type")
	public String getTokenType() {
		return tokenType;
	}
	
}
