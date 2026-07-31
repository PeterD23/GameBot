package trustsystem;

import static gamebot.EvgIds.ADELE_MEMBER;
import static gamebot.EvgIds.ADMIN_ROLE;
import static gamebot.EvgIds.BOT_MEMBER;
import static gamebot.EvgIds.DANIEL_MEMBER;
import static gamebot.EvgIds.MEETUP_ROLE;
import static gamebot.EvgIds.MODERATOR_ROLE;
import static gamebot.EvgIds.PETE_MEMBER;
import static trustsystem.RecommendedActions.RecommendedAction.ADD_CONFIDENCE;
import static trustsystem.RecommendedActions.RecommendedAction.DELETE_MESSAGES;
import static trustsystem.RecommendedActions.RecommendedAction.DELETE_MESSAGES_AND_TIMEOUT;
import static trustsystem.RecommendedActions.RecommendedAction.IGNORE;
import static trustsystem.TrustSystem.TrustRating.EXCELLENT;
import static trustsystem.TrustSystem.TrustRating.HIGH;
import static trustsystem.TrustSystem.TrustRating.LOW;
import static trustsystem.TrustSystem.TrustRating.MEDIUM;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.Color;
import gamebot.EvgIds;
import gamebot.GameBot;
import misc.MessageCache;
import misc.RedisConnector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Loggers;

public class TrustSystem {

	public static TrustSystem trustSystem;
	private String trustKey = "gamebot:TrustFactors";

	public enum TrustRating {

		EXCELLENT(100, 5), HIGH(50, 4), MEDIUM(20, 2), LOW(10, 1);

		public int weight, protection;

		public boolean is(TrustRating... ratings) {
			return Arrays.asList(ratings).contains(this);
		}

		TrustRating(int weight, int protection) {
			this.weight = weight;
			this.protection = protection;
		}
	}

	public static TrustSystem get() {
		if (trustSystem != null)
			return trustSystem;
		return new TrustSystem();
	}

	public Mono<Void> createTrustDialog(MessageInteractionEvent event) {
		Snowflake uniqueId = Snowflake.of(Instant.now());
		return event.deferReply().withEphemeral(true)
				.then(event.getTargetMessage()
						.map(message -> {
			GameBot.gateway.on(SelectMenuInteractionEvent.class)
					.filter(e -> e.getCustomId().equals("trust-options-" + uniqueId.asString()))
					.filter(e -> e.getUser().getId().equals(event.getUser().getId())).take(1)
					.flatMap(select -> submitTrust(select, message)).timeout(Duration.ofMinutes(5))
					.onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then().subscribe();

			List<SelectMenu.Option> trustOptions = new ArrayList<>();
			trustOptions.add(
					SelectMenu.Option.of("Message is supportive of marginalised communities", "ally"));
			trustOptions.add(SelectMenu.Option.of("Message was really insightful and provided good advice", "advice"));
			trustOptions.add(SelectMenu.Option.of("Message suggests user is extremely friendly", "friendly"));
			trustOptions.add(SelectMenu.Option.of("Other reason not specified", "other"));
			return trustOptions;
		}).map(list -> Container.of(Color.RED, TextDisplay.of("What makes you trust this person?"),
				ActionRow.of(
						SelectMenu.of("trust-options-" + uniqueId.asString(), list).withMinValues(1).withMaxValues(1))))
				.flatMap(container -> event.editReply().withComponents(container))).then();
	}

	private Mono<Void> submitTrust(SelectMenuInteractionEvent event, Message message) {
		return event.deferEdit().withEphemeral(true).then(
				event.getUser().asMember(Snowflake.of(GameBot.SERVER), EntityRetrievalStrategy.STORE_FALLBACK_REST))
				.flatMap(member -> {
					String trustedUser = message.getAuthor().get().getId().asString();
					String memberId = member.getId().asString();
					if(trustedUser.equals(memberId))
						return Mono.just("Nice try, but that loophole ain't going to work.");
					return getTrustFactors(member)
							.map(factors -> factors.stream()
									.anyMatch(factor -> factor.submittedBy.contains(memberId)))
							.flatMap(alreadyTrusted -> alreadyTrusted ? Mono.just("You've already trusted this user!")
									: getTrustScore(member)
											.flatMap(score -> addTrustFactor(Arrays.asList(trustedUser),
													new TrustFactor(memberId, "User is trusted", score/100)))
											.then(Mono.just("Submitted trust!")));
				}).flatMap(response -> event.editReply().withComponents(TextDisplay.of(response))).then();
	}

	public Mono<Void> createReportDialog(MessageInteractionEvent event) {
		Snowflake uniqueId = Snowflake.of(Instant.now());
		String userId = event.getUser().getId().asString();
		return event.deferReply().withEphemeral(true).then(existingReport(event.getResolvedMessage()))
				.defaultIfEmpty(Optional.empty()).flatMap(reported -> {
					if (alreadyReported(reported, userId))
						return event.editReply().withComponents(TextDisplay.of("You've already reported this user!"))
								.then();
					return Mono.just(event.getResolvedMessage()).map(message -> {
						ReportedUser user = reported.isPresent() ? reported.get().report(message)
								: ReportedUser.create(event.getUser()).report(message);

						GameBot.gateway.on(SelectMenuInteractionEvent.class)
								.filter(e -> e.getCustomId().equals("report-options-" + uniqueId.asString()))
								.filter(e -> e.getUser().getId().equals(event.getUser().getId())).take(1)
								.flatMap(select -> confirmReport(select, user, uniqueId)).timeout(Duration.ofMinutes(5))
								.onErrorComplete().then().subscribe();

						List<SelectMenu.Option> reportOptions = new ArrayList<>();
						reportOptions.add(SelectMenu.Option.of("Message is racist, sexist, or lgbtphobic", "bigotry"));
						reportOptions.add(
								SelectMenu.Option.of("Message is a personal attack or threatens me/a user", "attack"));
						reportOptions.add(SelectMenu.Option.of("Argument involving politics", "politics"));
						reportOptions
								.add(SelectMenu.Option.of("Self-promotion outside the rules of the discord", "spam"));
						reportOptions.add(SelectMenu.Option.of("Other reason not specified", "other"));
						return reportOptions;
					}).map(list -> Container.of(Color.RED,
							TextDisplay.of("Select the type of report that closest matches this message:"),
							ActionRow.of(SelectMenu.of("report-options-" + uniqueId.asString(), list).withMinValues(1)
									.withMaxValues(1))))
							.flatMap(container -> event.editReply().withComponents(container)).then();
				})
				.onErrorResume(
						t -> event.editReply().withComponents(TextDisplay.of("Failed due to " + t.getMessage())).then())
				.log(Loggers.getLogger("ReportDialog"));

	}

	private boolean alreadyReported(Optional<ReportedUser> user, String reportingUser) {
		if (Snowflake.of(reportingUser).equals(EvgIds.PETE_MEMBER.snow()))
			return false;
		return user.isPresent() ? user.get().wasReportedBy(reportingUser) : false;
	}

	private Mono<Optional<ReportedUser>> existingReport(Message target) {
		return RedisConnector
				.readValue("gamebot:ReportedUsers", target.getUserData().id().asString(), ReportedUser.class).log();
	}

	private Mono<Void> confirmReport(SelectMenuInteractionEvent event, ReportedUser user, Snowflake uniqueId) {
		String reason = event.getValues().get(0);

		GameBot.gateway.on(ButtonInteractionEvent.class).log(Loggers.getLogger("ConfirmReportDialog"))
				.filter(e -> e.getCustomId().contains(uniqueId.asString()))
				.filter(e -> e.getUser().getId().equals(event.getUser().getId())).take(1)
				.flatMap(button -> onButtonInteraction(button, user.because(reason))).timeout(Duration.ofMinutes(5))
				.onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then().subscribe();

		return event.deferEdit().withEphemeral(true)
				.then(Mono
						.fromCallable(() -> Container.of(Color.RED, TextDisplay.of("Confirm your report here"),
								Separator.of(), TextDisplay.of(user.recentReport.contentOrImages()),
								Separator.of(), TextDisplay.of("With reason: '" + reason + "'"),
								ActionRow.of(Button.danger("submit-report-" + uniqueId.asString(), "Submit Report"),
										Button.secondary("cancel-report-" + uniqueId.asString(), "Cancel Report"))))
						.flatMap(container -> event.editReply().withComponents(container)))
				.then();
	}

	private Mono<Void> onButtonInteraction(ButtonInteractionEvent event, ReportedUser finalReport) {
		if (!event.getCustomId().contains("report"))
			return Mono.empty();
		if (event.getCustomId().contains("cancel-report"))
			return event.deferEdit().withEphemeral(true).then(event.editReply().withComponents(TextDisplay.of("Report has been cancelled."))).then();
		return event.deferEdit().withEphemeral(true).then(sendReport(event, finalReport))
				.then(event.editReply().withComponents(TextDisplay.of("Thanks for your report."))).then();
	}

	private Mono<?> sendReport(ComponentInteractionEvent event, ReportedUser report) {
		String reporter = event.getUser().getId().asString();
		String reportedUser = report.getReported();
		return RedisConnector.mergeObject("gamebot:ReportedUsers", reportedUser, ReportedUser.class, report)
				.then(event.getInteraction().getGuild())
				.zipWhen(guild -> guild.getMemberById(Snowflake.of(reporter),
						EntityRetrievalStrategy.STORE_FALLBACK_REST))
				.zipWhen(tuple -> tuple.getT1().getMemberById(Snowflake.of(reportedUser),
						EntityRetrievalStrategy.STORE_FALLBACK_REST))
				.flatMap(tuple -> doRecommendedAction(report, tuple.getT1().getT2(), tuple.getT2())).then();
	}

	private Mono<?> doRecommendedAction(ReportedUser report, Member accuser, Member accused) {
		return getTrustScore(accuser).zipWith(getTrustScore(accused)).flatMap(pair -> {
			TrustRating accuserTrust = rating(pair.getT1());
			TrustRating accusedTrust = rating(pair.getT2());
			report.rating = accusedTrust;

			// Scenario 1: Accuser is High or Excellent, Accused is Low
			if ((accuserTrust.is(HIGH, EXCELLENT)) && accusedTrust.is(LOW)) {
				// Bigotry or Attack is a Delete + Timeout, others will be just Deleted and
				// Flagged
				if (report.reasons("bigotry", "attack"))
					return DELETE_MESSAGES_AND_TIMEOUT.go(report, accuserTrust).then(Mono.just(20));
				return DELETE_MESSAGES.go(report, accuserTrust);

				// Scenario 2: Accuser is Medium High or Excellent, Accused is Medium High or
				// Excellent
			} else if (accuserTrust.is(MEDIUM, HIGH, EXCELLENT) && (accusedTrust.is(MEDIUM, HIGH, EXCELLENT))) {
				// Add Confidence to Report based on accuser weight
				return ADD_CONFIDENCE.go(report, accuserTrust).ofType(Boolean.class)
						.flatMap(threshold -> threshold ? DELETE_MESSAGES.go(report, accuserTrust) : Mono.empty());

				// Scenario 3: Accuser is Medium, High or Excellent, Accused is Medium
			} else if (accuserTrust.is(MEDIUM, HIGH, EXCELLENT) && (accusedTrust == MEDIUM)) {
				return ADD_CONFIDENCE.go(report, accuserTrust).ofType(Boolean.class)
						.flatMap(threshold -> threshold ? DELETE_MESSAGES.go(report, accuserTrust) : Mono.empty());

				// Scenario 4: Accuser is Medium, High, Accused is Low
			} else if (accuserTrust.is(LOW, MEDIUM) && accusedTrust.is(LOW)) {
				return ADD_CONFIDENCE.go(report, accuserTrust).ofType(Boolean.class).flatMap(
						threshold -> threshold ? DELETE_MESSAGES_AND_TIMEOUT.go(report, accuserTrust) : Mono.empty());
			}

			return IGNORE.go(report, accuserTrust);
		}).ofType(Integer.class)
				.flatMap(trustAward -> trustAward > 0 ? addTrustFactor(report.reportedBy,
						new TrustFactor("SERVER", "Successfully reported a message that was deleted", trustAward))
						.then(addTrustFactor(Arrays.asList(report.reportedUserId),
								new TrustFactor("SERVER", "Reported for messages that were deleted", -trustAward * 2)))
						: Mono.empty());

	}

	private TrustRating rating(int score) {
		if (score < 100)
			return LOW;
		if (score < 1000)
			return MEDIUM;
		if (score < 5000)
			return HIGH;
		return EXCELLENT;
	}

	private Mono<Integer> getTrustScore(Member member) {
		return getServerFactors(member).concatWith(getTrustFactors(member))
				.map(factors -> factors.stream().map(factor -> factor.score).reduce(0, Integer::sum))
				.reduce(Integer::sum);
	}

	public Mono<Void> addTrustFactor(List<String> accusers, TrustFactor factor) {
		return Flux.fromIterable(accusers).flatMap(accuser -> RedisConnector.appendEntry(trustKey, accuser, factor))
				.then();
	}

	// Collapse duplicates and sum up
	public Mono<List<TrustFactor>> getTrustFactors(Member member) {
		return RedisConnector.readList(trustKey, member.getId().asString(), TrustFactor.class).map(factors -> 
			factors.stream()
			.collect(Collectors.groupingBy(TrustFactor::why, Collectors.summingInt(TrustFactor::howMuch)))
			.entrySet()
			.stream()
			.map(entry -> TrustFactor.of(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList())
		);
	}

	public Mono<List<TrustFactor>> getServerFactors(Member member) {
		return Flux.just(isWho(member),
				discordAccountAge(member),
				evgAccountAge(member))
				.mergeWith(calculateMessageHistory(member))
				.mergeWith(getRelevantRoles(member)
						.flatMapMany(flat -> Flux.fromIterable(flat)))
				.collectList();
	}

	private Mono<ArrayList<TrustFactor>> getRelevantRoles(Member member) {
		TrustFactor adminMod = TrustFactor.of("Is an Admin/Moderator", 5000);
		ArrayList<TrustFactor> factors = new ArrayList<>();
		return member.getRoles(EntityRetrievalStrategy.STORE_FALLBACK_REST).flatMap(role -> {

			if ((role.getId().equals(MODERATOR_ROLE.snow())
					|| role.getId().equals(ADMIN_ROLE.snow()) && !factors.contains(adminMod))) {
				factors.add(adminMod);
			}

			if (role.getId().equals(MEETUP_ROLE.snow())) {
				factors.add(TrustFactor.of("Is Meetup Verified", 100));
			}

			return Flux.empty();
		}).then(Mono.just(factors));
	}

	private TrustFactor isWho(Member member) {
		Snowflake id = member.getId();
		if (id.equals(BOT_MEMBER.snow())) {
			return TrustFactor.of("I'm literally a robot", 0);
		}
		if (id.equals(PETE_MEMBER.snow())) {
			return TrustFactor.of("Creator of the trust system", -1);
		}
		if (id.equals(ADELE_MEMBER.snow())) {
			return TrustFactor.of("Creator of EVG", 10000000);
		}
		if (id.equals(DANIEL_MEMBER.snow())) {
			return TrustFactor.of("Daniel", 10000000);
		}
		return TrustFactor.empty();
	}

	private TrustFactor evgAccountAge(Member member) {
		Optional<Instant> userJoinTime = member.getJoinTime();

		if (!userJoinTime.isPresent())
			return TrustFactor.of("Apparently never joined the server", -100);	

		long accountAge = ChronoUnit.DAYS.between(userJoinTime.get(), Instant.now());
		long daysAfterServer = Math
				.abs(ChronoUnit.DAYS.between(userJoinTime.get(), Snowflake.of(GameBot.SERVER).getTimestamp()));

		if (daysAfterServer < 30)
			return TrustFactor.of("Joined Edinburgh Video Gamers in its first month! (" + accountAge + " days)", 500);
		else if (accountAge > 1825)
			return TrustFactor.of("Has been on Edinburgh Video Gamers for over 5 years (" + accountAge + " days)", 250);
		else if (accountAge > 365)
			return TrustFactor.of("Has been on Edinburgh Video Gamers for over a year (" + accountAge + " days)", 100);
		else if (accountAge > 30)
			return TrustFactor.of("Has been on Edinburgh Video Gamers for at least a month (" + accountAge + " days)", 10);
		else if (accountAge < 14)
			return TrustFactor.of("Has just joined Edinburgh Video Gamers", 0);

		return TrustFactor.empty();
	}

	private TrustFactor discordAccountAge(Member member) {
		Instant userCreationTime = member.getId().getTimestamp();
		long accountAge = ChronoUnit.DAYS.between(userCreationTime, Instant.now());
		if (accountAge > 3650) {
			return TrustFactor.of("Has been on Discord for over 10 years (" + accountAge + " days)", 100);
		} else if (accountAge > 1825) {
			return TrustFactor.of("Has been on Discord for over 5 years (" + accountAge + " days)", 75);
		} else if (accountAge > 365) {
			return TrustFactor.of("Has been on Discord for over a year (" + accountAge + " days)", 50);
		} else if (accountAge > 30) {
			return TrustFactor.of("Has been on Discord for over a month (" + accountAge + " days)", 0);
		} else if (accountAge < 14) {
			return TrustFactor.of("Has just created a new account", -100);
		}
		return TrustFactor.empty();
	}

	private Mono<TrustFactor> calculateMessageHistory(Member member) {
		return MessageCache.getUserFromCache(member.getId().asString()).map(list -> {

			int trust = 0;
			List<Integer> dates = list.stream().map(message -> getDayFromZero(message.timestamp))
					.collect(Collectors.toList());

			long noOfDays = dates.stream().distinct().count();

			int day = 0;
			int tempTrust = 0;
			for (Integer date : dates) {
				tempTrust = Math.min(5, tempTrust + 1);
				if (day != date) {
					trust += tempTrust;
					day = date;
					tempTrust = 0;
				}
			}

			return TrustFactor.of("Contributed " + list.size() + " messages over " + noOfDays + " days", trust);
		});
	}

	private int getDayFromZero(String timestamp) {
		LocalDate date = LocalDate.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return date.get(ChronoField.DAY_OF_YEAR) * date.get(ChronoField.YEAR);
	}
}
