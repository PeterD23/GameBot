package gamebot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
public interface ISlashCommand {

	Mono<Void> submitCommand(ChatInputInteractionEvent event);
	
	ApplicationCommandRequest getCommandRequest();
	
	default String desc() {
		return "alan please add";
	}
	
	default Mono<Void> onModalSubmit(ModalSubmitInteractionEvent event) {
		return null;
	};
}