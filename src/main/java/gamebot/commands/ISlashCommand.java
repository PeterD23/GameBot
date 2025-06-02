package gamebot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
public interface ISlashCommand {

	Mono<Void> submitCommand(ChatInputInteractionEvent event);
	
	default String desc() {
		return "alan please add";
	}
	
	default Mono<Void> onModalSubmit(ModalSubmitInteractionEvent event) {
		return null;
	};
}