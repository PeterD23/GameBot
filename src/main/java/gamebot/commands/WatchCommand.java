package gamebot.commands;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;	
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.GameBot;
import reactor.core.publisher.Mono;

public class WatchCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	private long POLL_WATCHER = 908032897762619433L;
	private long EVENT_WATCHER = 1367632092694839427L;
	public static WatchCommand command;

	public static WatchCommand get() {
		if (command == null) {
			command = new WatchCommand();
		}
		return command;
	}
	
	public String desc() {
		return "**/watch** Shows two buttons allowing you to subscribe or unsubscribe from Event and Poll pings."; 
	}

	private Role getRoleById(long id) {
		return GameBot.gateway.getGuildById(Snowflake.of(guildId)).block().getRoles()
				.filter(p -> p.getId().asLong() == id).next().block();
	}

	private boolean hasRole(Member member, long roleId) {
		Snowflake id = getRoleById(roleId).getId();
		return member.getRoleIds().contains(id);
	}

	public WatchCommand() {
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();

		ApplicationCommandRequest watchRequest = ApplicationCommandRequest.builder().name("watch")
				.description("Watch for polls or events").build();

		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, watchRequest).subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		GameBot.gateway.on(ButtonInteractionEvent.class, click -> onButtonClick(click)).timeout(Duration.ofMinutes(5))
				.onErrorResume(TimeoutException.class, ignore -> Mono.empty()).then().subscribe();

		return event.reply("Click these buttons in order to subscribe or unsubscribe from Polls or Events!")
				.withComponents(ActionRow.of(generatePollButton(member), generateEventButton(member)))
				.withEphemeral(true);
	}

	private Button generateEventButton(Member member, long roleId, boolean enabled) {
		if (roleId == EVENT_WATCHER) {
			return button("event-button", "Events", enabled);
		}
		return generateEventButton(member);
	}

	private Button generateEventButton(Member member) {
		return button("event-button", "Events", hasRole(member, EVENT_WATCHER));
	}

	private Button generatePollButton(Member member, long roleId, boolean enabled) {
		if (roleId == POLL_WATCHER) {
			return button("poll-button", "Polls", enabled);
		}
		return generatePollButton(member);
	}

	private Button generatePollButton(Member member) {
		return button("poll-button", "Polls", hasRole(member, POLL_WATCHER));
	}
	
	private Button button(String id, String name, boolean enabled) {
		return enabled ? Button.success(id, name) : Button.danger(id,name);
	}

	public Mono<Void> onButtonClick(ButtonInteractionEvent event) {
		Member member = event.getInteraction().getMember().get();
		String data = event.getCustomId();
		String watch = data.equals("poll-button") ? "Poll" : "Event";
		long roleId = data.equals("poll-button") ? POLL_WATCHER : EVENT_WATCHER;

		if (!hasRole(member, roleId)) {
			member.addRole(getRoleById(roleId).getId(), "Requested by user").block();
			return event.edit("Hey, you will now be pinged whenever a new " + watch + " is created!")
						.withComponents(ActionRow.of(generatePollButton(member, roleId, true), generateEventButton(member, roleId, true)));
		}
		member.removeRole(getRoleById(roleId).getId(), "Requested by user").block();
		return event.edit("Hey, you will no longer be pinged whenever a new " + watch + " is created!")
						.withComponents(ActionRow.of(generatePollButton(member, roleId, false), generateEventButton(member, roleId, false)));
	}
}
