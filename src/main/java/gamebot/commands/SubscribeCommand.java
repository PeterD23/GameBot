package gamebot.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import meetup.selenium.Tuple;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SubscribeCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	private ArrayList<Tuple<String, Long, String>> genreRoles = new ArrayList<>();
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
	
	private Mono<GuildEmoji> getEmojiByName(String name) {
		return GameBot.gateway
				.getGuildById(Snowflake.of(guildId))
				.flatMap(guild -> guild.getEmojis()
					.filter(p -> p.getName().equals(name))
					.next());
	}

	private Mono<String> getRoleName(long id) {
		return GameBot.gateway
				.getGuildById(Snowflake.of(guildId))
				.flatMap(guild -> guild.getRoles()
					.filter(p -> p.getId().asLong() == id)
					.next()
					.map(role -> role.getName()));
	}

	private boolean hasRole(Member member, long roleId) {
		Snowflake id = Snowflake.of(roleId);
		return member.getRoleIds().contains(id);
	}
	
	public Mono<Void> readDataIntoTuple(String fileName) {
		genreRoles.clear();
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), Charset.defaultCharset());
			return Mono.when(Flux.fromIterable(lines).flatMap(line -> {
				String[] data = line.split(" ");
				return getRoleName(new Long(data[1]))
						.flatMap(roleName -> Mono.fromRunnable(() -> genreRoles.add(new Tuple<>(data[0], new Long(data[1]), roleName)))); 
			}));
		} catch (IOException e) {
			return ChannelLogger.logMessageError("Failed to read genres file:", e);
		}
	}
	
	private Mono<Void> addOrRemoveRole(List<Long> values, Member member, long role){
		if(values.contains(role) && !hasRole(member, role))
			return member.addRole(Snowflake.of(role));
		else if(!values.contains(role) && hasRole(member, role))
			return member.removeRole(Snowflake.of(role));
		return Mono.empty();
	}
	
	private Mono<SelectMenu> constructRoleMenu(Member member) {
		ArrayList<SelectMenu.Option> roles = new ArrayList<>();
		return Mono.when(
				Flux.fromIterable(genreRoles)
					.flatMap(tup -> getEmojiByName(tup.first())
						.flatMap(emoji -> Mono.fromRunnable(
								() -> roles.add(SelectMenu.Option.of(tup.third(), tup.second().toString())
								.withEmoji(ReactionEmoji.custom(emoji))
								.withDefault(hasRole(member, tup.second())))
							)
						)
					)
				)
				.then(Mono.fromCallable(() -> SelectMenu.of("role-menu", roles)
					.withMinValues(0)
					.withMaxValues(roles.size())));
	}	

	public SubscribeCommand() {
		ApplicationCommandRequest subscribeRequest = ApplicationCommandRequest.builder().name("subscribe")
				.description("Subscribe to genre channels").build();
		
		GatewayDiscordClient client = GameBot.gateway;
		client.getRestClient()
		.getApplicationId()
		.flatMap(applicationId -> client.getRestClient()
			.getApplicationService()
			.createGuildApplicationCommand(applicationId, guildId, subscribeRequest))	
		.then(readDataIntoTuple("genres"))
		.subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		GameBot.gateway.on(SelectMenuInteractionEvent.class, select -> onSelectInteraction(select)).timeout(Duration.ofMinutes(5))
				.onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then().subscribe();
		
		return event.deferReply()
				.withEphemeral(true)
				.then(constructRoleMenu(member)
						.flatMap(roleMenu -> 
							event.editReply()
								.withComponents(
									Container.of(
										TextDisplay.of("Select which genres you're interested in!"),
										Separator.of(),
										ActionRow.of(roleMenu)
									))))
				.then();
	}

	public Mono<Void> onSelectInteraction(SelectMenuInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		List<Long> values = event.getValues().stream().map(m -> Long.parseLong(m)).collect(Collectors.toList());
		List<Long> genres = genreRoles.stream().map(roles -> roles.second()).collect(Collectors.toList());
		
		return event.deferEdit()
				.then(Mono.when
					(Flux.fromIterable(genres)
						.flatMap(role -> addOrRemoveRole(values, member, role)))
					.then(event.editReply()
				.withComponents(TextDisplay.of("# Thank you for your submission!"))))
				.then();
	}
}
