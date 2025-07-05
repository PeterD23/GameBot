package gamebot.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Color;
import gamebot.GameBot;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import trustsystem.TrustFactor;
import trustsystem.TrustSystem;

public class TrustCommand implements ISlashCommand {

	private boolean admin = false;

	public TrustCommand(boolean admin) {
		this.admin = admin;
	}

	@Override
	public String desc() {
		return "**/trust** Checks your trust score";
	}

	public ImmutableApplicationCommandRequest getCommandRequest() {
		return admin ? getAdminCommandRequest()
				: ApplicationCommandRequest.builder().name("trust")
						.description("Get your trust score, better than credit!").build();
	}

	public static ImmutableApplicationCommandRequest getAdminCommandRequest() {		
		return ApplicationCommandRequest.builder().name("see-trust")
				.description("See another users credi-I mean trust score")
				.addOption(ApplicationCommandOptionData.builder().name("user")
						.description("Who are you peeking at? :eyes:")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true).build())
				.build();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		TrustSystem trust = TrustSystem.get();
		Member self = event.getInteraction().getMember().get();
		return event.deferReply()
				.withEphemeral(!admin)
				.then(event.getOption("user").isPresent()
						? event.getOptionAsUser("user")
								.flatMap(option -> option.asMember(Snowflake.of(GameBot.SERVER),
										EntityRetrievalStrategy.STORE_FALLBACK_REST))
						: Mono.just(self))
				.flatMap(member -> trust.getServerFactors(member).zipWith(trust.getTrustFactors(member))
					.flatMap(pair -> event.editReply().withComponents(constructMessage(member, pair))))
				.then();
	}

	private TopLevelMessageComponent constructMessage(Member member, Tuple2<List<TrustFactor>, List<TrustFactor>> factors) {
		List<TrustFactor> totalFactors = factors.getT1(); 
		totalFactors.addAll(factors.getT2()); 
		
		// Map all factor pairs into a string
		List<String> stringedFactors = totalFactors.stream().map(factor -> factor.reason + ": " + factor.score)
				.collect(Collectors.toList());

		// Sum all factors into a final trust score
		int trustScore = totalFactors.stream().map(factor -> factor.score).reduce(0, Integer::sum);

		// Print a message based on that trust score
		Pair<Color, String> finalTrust = getTrustRating(trustScore);

		return Container.of(finalTrust.getLeft(),
				TextDisplay.of("Alright " + member.getMention() + ", time for judgement..."), Separator.of(),
				TextDisplay.of(String.join("\n", stringedFactors)), Separator.of(),
				TextDisplay.of(finalTrust.getRight()));
	}

	private Pair<Color, String> getTrustRating(int trustScore) {
		if (trustScore < 100) {
			return Pair.of(Color.RED, "Your trust rating is **LOW** (" + trustScore
					+ ") If you receive reports from trusted users you will incur a timeout and possibly a ban.");
		}
		if (trustScore < 1000) {
			return Pair.of(Color.YELLOW, "Your trust rating is **MEDIUM** (" + trustScore
					+ ") You are a moderately active contributor to the discord!");
		}
		if (trustScore < 5000) {
			return Pair.of(Color.GREEN, "Your trust rating is **HIGH** (" + trustScore
					+ ") You are an active member of this discord and your reports will carry significant weight!");
		}
		return Pair.of(Color.CYAN, "Your trust rating is **EXCELLENT** (" + trustScore
				+ ") You are either very well liked by the community or you're a moderator :fire:");
	}

	
}
