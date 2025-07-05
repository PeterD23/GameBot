package gamebot.commands;

import static discord4j.common.store.action.read.ReadActions.getRoles;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import discord4j.common.store.Store;
import discord4j.common.store.action.read.ReadActions;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.EmojiData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.discordjson.json.RoleData;
import gamebot.GameBot;
import misc.RedisConnector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SubscribeCommand implements ISlashCommand {

	private String key = "gamebot:GameGenres";
	private HashMap<String, String> genreRoles = new HashMap<>();
	public static SubscribeCommand command;

	public static SubscribeCommand get() {
		if (command == null) {
			command = new SubscribeCommand();
		}
		return command;
	}

	public String desc() {
		return "**/subscribe** Bring up a multi-select menu for choosing game genres to subscribe to.";
	}

	private Mono<RoleData> getRoleData(String id) {
		Store store = GameBot.gateway.getGatewayResources().getStore();
		return Flux.from(store.execute(getRoles())).filter(p -> p.id().asString().equals(id)).next();
	}

	private Mono<EmojiData> getEmojiByName(String name) {
		Store store = GameBot.gateway.getGatewayResources().getStore();
		return Flux.from(store.execute(ReadActions.getEmojis())).filter(p -> p.name().get().equals(name)).next();
	}

	private boolean hasRole(Member member, String roleId) {
		Snowflake id = Snowflake.of(roleId);
		return member.getRoleIds().contains(id);
	}

	public Mono<Void> readGenres() {
		return RedisConnector.cacheFile(new File("genres"), key).flatMap(map -> {
			genreRoles = map;
			return Mono.empty();
		}).then();
	}

	private Mono<Void> addOrRemoveRole(List<String> values, Member member, String role) {
		if (values.contains(role) && !hasRole(member, role))
			return member.addRole(Snowflake.of(role));
		else if (!values.contains(role) && hasRole(member, role))
			return member.removeRole(Snowflake.of(role));
		return Mono.empty();
	}

	private Mono<SelectMenu> constructRoleMenu(Member member) {
		return Flux.fromIterable(genreRoles.entrySet()).checkpoint("Flux.constructRoleMenu").flatMap(entry -> {
			return getEmojiByName(entry.getKey()).zipWith(getRoleData(entry.getValue()))
					.map(pair -> SelectMenu.Option.of(pair.getT2().name(), entry.getValue())
							.withEmoji(ReactionEmoji.of(pair.getT1())).withDefault(hasRole(member, entry.getValue())));
		}).collectList()
				.map(options -> SelectMenu.of("role-menu", options).withMinValues(0).withMaxValues(options.size()));
	}

	public ImmutableApplicationCommandRequest getCommandRequest() {
		return ApplicationCommandRequest.builder().name("subscribe").description("Subscribe to genre channels")
						.build();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		GameBot.gateway.on(SelectMenuInteractionEvent.class, select -> onSelectInteraction(select))
				.timeout(Duration.ofMinutes(5)).onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then()
				.subscribe();

		return event.deferReply()
				.withEphemeral(true)
				.then(
						constructRoleMenu(member)
						.flatMap(roleMenu -> event.editReply()
						.withComponents(Container.of(TextDisplay.of("Select which genres you're interested in!"),
								Separator.of(), ActionRow.of(roleMenu)))))
				.then();
	}

	public Mono<Void> onSelectInteraction(SelectMenuInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		List<String> values = event.getValues();
		List<String> genres = genreRoles.entrySet().stream().map(roles -> roles.getValue())
				.collect(Collectors.toList());

		return event.deferEdit()
				.then(Mono.when(Flux.fromIterable(genres).flatMap(role -> addOrRemoveRole(values, member, role)))
						.then(event.editReply().withComponents(TextDisplay.of("# Thank you for your submission!"))))
				.then();
	}
}
