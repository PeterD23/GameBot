package gamebot.commands.admin;

import java.util.ArrayList;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import gamebot.ICommand;
import gamebot.Utils;
import gamebot.commands.ISlashCommand;
import reactor.core.publisher.Mono;

public class CallbackCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	private ICommand callback;
	protected long ADMIN_ROLE = 731604497435983992L;

	private String name;
	private String description;
	private ArrayList<ApplicationCommandOptionData> options = new ArrayList<>();

	protected boolean isAdmin(Member usr, ChatInputInteractionEvent event) {
		if (Utils.adminsDenied() && !event.getCommandName().equals("deny"))
			return false;
		return usr.getRoles().any(p -> p.getId().asLong() == ADMIN_ROLE).block().booleanValue();
	}

	public CallbackCommand(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public CallbackCommand withCallBack(ICommand callback) {
		this.callback = callback;
		return this;
	}

	public CallbackCommand withIntArg(String name, String desc, double minValue, double maxValue) {
		options.add(ApplicationCommandOptionData.builder().name(name).description(desc)
				.type(ApplicationCommandOption.Type.INTEGER.getValue()).required(true).minValue(minValue)
				.maxValue(maxValue).build());
		return this;
	}

	public CallbackCommand withStringArg(String name, String desc, int minLength, int maxLength) {
		options.add(ApplicationCommandOptionData.builder().name(name).description(desc)
				.type(ApplicationCommandOption.Type.STRING.getValue()).required(true).minLength(minLength)
				.maxLength(maxLength).build());
		return this;
	}

	public CallbackCommand create() {
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();

		ApplicationCommandRequest callbackRequest = ApplicationCommandRequest.builder().name(name)
				.description(description).addAllOptions(options).build();

		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, callbackRequest)
				.then(Mono.fromRunnable(
						() -> ChannelLogger.logMessageInfo("Successfully registered admin command '" + name + "'")))
				.subscribe();
		return this;
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
