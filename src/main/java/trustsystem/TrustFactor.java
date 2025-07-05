package trustsystem;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TrustFactor {

	public String submittedBy;
	public String reason;
	public int score;
	
	public TrustFactor() {}
	
	@JsonIgnore
	public static TrustFactor of(String reason, int score) {
		return new TrustFactor("SERVER", reason, score);
	}
	
	public TrustFactor(String submittedBy, String reason, int score) {
		this.submittedBy = submittedBy;
		this.reason = reason;
		this.score = score;
	}
	
	@JsonIgnore
	public String why() {
		return reason;
	}
	
	@JsonIgnore
	public int howMuch() {
		return score;
	}

	@JsonIgnore
	public static TrustFactor empty() {
		return new TrustFactor("","",0);
	}
	
	@JsonIgnore
	public boolean isEmpty() {
		return submittedBy.equals("");
	}
	
}
