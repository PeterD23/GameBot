package gamebot.commands.admin;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import gamebot.ICommand;
import gamebot.Utils;
import gamebot.commands.ISlashCommand;
import reactor.core.publisher.Mono;

public class CallbackNoArgCommand implements ISlashCommand {
	
	private long guildId = GameBot.SERVER;
	private ICommand callback;
	protected long ADMIN_ROLE = 731604497435983992L;
	
	protected boolean isAdmin(Member usr, ChatInputInteractionEvent event) {
		if (Utils.adminsDenied() && !event.getCommandName().equals("deny"))
			return false;
		return usr.getRoles().any(p -> p.getId().asLong() == ADMIN_ROLE).block().booleanValue();
	}
	
	public CallbackNoArgCommand(String name, String description, ICommand callback) {
		this.callback = callback;
		
		ApplicationCommandRequest callbackNoArgRequest = ApplicationCommandRequest.builder().name(name)
				.description(description).build();
		
		GatewayDiscordClient client = GameBot.gateway;
		client.getRestClient().getApplicationId().flatMap(applicationId -> {
			return client.getRestClient().getApplicationService()
					.createGuildApplicationCommand(applicationId, guildId, callbackNoArgRequest)
					.then()
					.onErrorResume(t -> ChannelLogger.logMessageError("Failed to register admin command '"+name+"'", t)
					);
		}).subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		Member usr = event.getInteraction().getMember().get();
		if(!isAdmin(usr, event)) {
			return event.reply("You are not authorised to use this command.");
		}
		return callback.execute(event);
	}

}
