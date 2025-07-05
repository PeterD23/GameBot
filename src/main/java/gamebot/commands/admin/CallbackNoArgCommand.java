package gamebot.commands.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import gamebot.ICommand;
import gamebot.Utils;
import gamebot.commands.ISlashCommand;
import reactor.core.publisher.Mono;

public class CallbackNoArgCommand implements ISlashCommand {

	private ICommand callback;
	protected long ADMIN_ROLE = 731604497435983992L;

	private String name;
	private String description;

	protected boolean isAdmin(Member usr, ChatInputInteractionEvent event) {
		if (Utils.adminsDenied() && !event.getCommandName().equals("deny"))
			return false;
		return usr.getRoleIds().contains(Snowflake.of(ADMIN_ROLE));
	}

	public CallbackNoArgCommand(String name, String description, ICommand callback) {
		this.name = name;
		this.description = description;
		this.callback = callback;
	}

	public ImmutableApplicationCommandRequest getCommandRequest() {
		return ApplicationCommandRequest.builder().name(name).description(description).build();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member usr = event.getInteraction().getMember().get();
		if (!isAdmin(usr, event)) {
			return event.reply("You are not authorised to use this command.");
		}
		return callback.execute(event);
	}

}
