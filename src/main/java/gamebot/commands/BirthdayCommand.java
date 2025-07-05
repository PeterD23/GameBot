package gamebot.commands;

import java.time.LocalDate;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import misc.Birthday;
import reactor.core.publisher.Mono;

public class BirthdayCommand implements ISlashCommand {

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

	public ImmutableApplicationCommandRequest getCommandRequest() {
		return ApplicationCommandRequest.builder().name("birthday").description("Tell me its your birthday today!")
				.build();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		LocalDate today = LocalDate.now();
		Member member = event.getInteraction().getMember().get();
		String user = member.getId().asString();
		return Birthday.add(user, today)
				.then(event.reply("Happy Birthday " + member.getMention() + "! I will remember this."));
	}

	@Override
	public Mono<Void> onModalSubmit(ModalSubmitInteractionEvent event) {
		return Mono.empty();
	}
}
