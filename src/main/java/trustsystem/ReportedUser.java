package trustsystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import misc.Mergeable;
import misc.Utils;
import trustsystem.TrustSystem.TrustRating;

public class ReportedUser implements Mergeable<ReportedUser> {

	public ArrayList<String> reportedBy = new ArrayList<>();
	public ArrayList<ReportedMessage> reportedMessages = new ArrayList<>();
	public ReportedMessage recentReport;
	public TrustRating rating;
	public int confidence = 0;
	public String reportId;
	public String reportedUserId;

	@JsonIgnore
	private boolean merge = true;

	public ReportedUser() {
	}

	private ReportedUser(Snowflake invokerId) {
		reportedBy = new ArrayList<>();
		reportedBy.add(invokerId.asString());
	}

	public static ReportedUser create(User reporter) {
		return new ReportedUser(reporter.getId());
	}

	@JsonIgnore
	public ReportedUser report(Message message) {
		ReportedMessage report = new ReportedMessage(message);

		// Add to list if not present, otherwise merge
		combineReportedMessage(report);

		reportedUserId = message.getUserData().id().asString();
		recentReport = report;
		return this;
	}

	@JsonIgnore
	public ReportedUser clear() {
		reportedBy.clear();
		reportedMessages.clear();
		recentReport = null;
		confidence = 0;
		// Will overwrite the cached report on next write
		merge = false;
		return this;
	}

	@JsonIgnore
	public boolean addConfidence(TrustRating accuser) {
		confidence += accuser.weight / rating.protection;
		return confidence >= 100;
	}

	@JsonIgnore
	public boolean wasReportedBy(String user) {
		return reportedBy.contains(user);
	}

	public ReportedUser because(String reason) {
		recentReport.reasons.add(reason);
		combineReportedMessage(recentReport);
		return this;
	}

	@JsonIgnore
	public String getReported() {
		return reportedUserId;
	}

	public boolean reasons(String... reasons) {
		for (ReportedMessage reported : reportedMessages) {
			if (!Collections.disjoint(reported.reasons(), Arrays.asList(reasons)))
				return true;
		}
		return false;
	}

	private void combineReportedMessage(ReportedMessage toMerge) {
		Optional<ReportedMessage> optional = reportedMessages.stream()
				.filter(message -> message.messageId.equals(toMerge.messageId)).findFirst();
		if (optional.isPresent())
			optional.get().combineIfEquals(toMerge);
		else
			reportedMessages.add(toMerge);
	}

	@JsonIgnore
	@Override
	public ReportedUser merge(ReportedUser mergeable) {
		if (merge) {
			reportedBy = Utils.combineDistinct(reportedBy, mergeable.reportedBy);
			confidence = Math.max(confidence, mergeable.confidence);
			if (reportId == null && mergeable.reportId != null)
				reportId = mergeable.reportId;
			if (rating == null && mergeable.rating != null)
				rating = mergeable.rating;

			// Combining the attributes of elements in two lists is harder than it might
			// initially seem
			for (ReportedMessage toMerge : mergeable.reportedMessages) {
				combineReportedMessage(toMerge);
			}
		}
		merge = true;
		return this;
	}

}
