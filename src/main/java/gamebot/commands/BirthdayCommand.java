package gamebot.commands;

import java.time.LocalDate;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.GameBot;
import misc.Birthday;
import reactor.core.publisher.Mono;

public class BirthdayCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	public static BirthdayCommand command;

	public static BirthdayCommand get() {
		if (command == null) {
			command = new BirthdayCommand();
		}
		return command;
	}
	
	@Override
	public String desc() {
		return "**/birthday** Lets me know its your birthday today, so you'll get a birthday greeting from me and I'll remember it for next time!";
	}

	public BirthdayCommand() {
		ApplicationCommandRequest birthdayRequest = ApplicationCommandRequest.builder().name("birthday")
				.description("Tell me its your birthday today!")
				.build();
		
		GatewayDiscordClient client = GameBot.gateway;
		client.getRestClient().getApplicationId()
			.flatMap(applicationId -> 
				client.getRestClient().getApplicationService()
					.createGuildApplicationCommand(applicationId, guildId, birthdayRequest))
			.subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		LocalDate today = LocalDate.now();
		Member member = event.getInteraction().getMember().get();
		
		long user = member.getId().asLong();
		Birthday.add(user, today);
		return event.reply("Happy Birthday "+ member.getMention()+"! I will remember this.");
	}

	@Override
	public Mono<Void> onModalSubmit(ModalSubmitInteractionEvent event) {
		return Mono.empty();
	}
}
