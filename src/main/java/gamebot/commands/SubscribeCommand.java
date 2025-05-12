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
import discord4j.core.object.entity.Role;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import meetup.selenium.Tuple;
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
	
	protected GuildEmoji getEmojiByName(String name) {
		return GameBot.gateway.getGuildById(Snowflake.of(guildId)).block().getEmojis().filter(p -> p.getName().equals(name)).next().block();
	}

	private Role getRoleById(long id) {
		return GameBot.gateway.getGuildById(Snowflake.of(guildId)).block().getRoles()
				.filter(p -> p.getId().asLong() == id).next().block();
	}

	private boolean hasRole(Member member, long roleId) {
		Snowflake id = Snowflake.of(roleId);
		return member.getRoleIds().contains(id);
	}
	
	public void readDataIntoTuple(String fileName) {
		genreRoles.clear();
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), Charset.defaultCharset());
			for (String line : lines) {
				String[] data = line.split(" ");
				genreRoles.add(new Tuple<>(data[0], new Long(data[1]), getRoleById(new Long(data[1])).getName()));
			}
		} catch (IOException e) {
			ChannelLogger.logMessageError("Failed to read genres file");
		}
	}
	
	private SelectMenu constructRoleMenu(Member member) {	
		List<SelectMenu.Option> roles = genreRoles.stream()
				.map(tup -> {
					ReactionEmoji emoji = ReactionEmoji.custom(getEmojiByName(tup.first()));
					return SelectMenu.Option
							.of(tup.third(), tup.second().toString())
							.withEmoji(emoji)
							.withDefault(hasRole(member, tup.second()));
				})
				.collect(Collectors.toList());
		return SelectMenu.of("role-menu", roles).withMinValues(0).withMaxValues(roles.size());
	}	

	public SubscribeCommand() {
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();

		ApplicationCommandRequest subscribeRequest = ApplicationCommandRequest.builder().name("subscribe")
				.description("Subscribe to genre channels").build();

		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, subscribeRequest).subscribe();
		readDataIntoTuple("genres");
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		GameBot.gateway.on(SelectMenuInteractionEvent.class, select -> onSelectInteraction(select)).timeout(Duration.ofMinutes(5))
				.onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then().subscribe();
		
		return event.reply()
				.withComponents(
						Container.of(
								TextDisplay.of("Select which genres you're interested in!"),
								Separator.of(),
								ActionRow.of(constructRoleMenu(member))
						))
				.withEphemeral(true);
	}

	public Mono<Void> onSelectInteraction(SelectMenuInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		List<Long> values = event.getValues().stream().map(m -> Long.parseLong(m)).collect(Collectors.toList());
		
		return event.deferEdit().then(Mono.fromRunnable(() -> {
			genreRoles.stream().map(roles -> roles.second()).collect(Collectors.toList()).forEach(role -> {
				if(values.contains(role) && !hasRole(member, role)) {
					member.addRole(Snowflake.of(role)).block();
				} else if(!values.contains(role) && hasRole(member, role)) {
					member.removeRole(Snowflake.of(role)).block();
				}
			});
		}).then(event.editReply()
				.withComponents(TextDisplay.of("# Thank you for your submission!")))).then();
	}
}
