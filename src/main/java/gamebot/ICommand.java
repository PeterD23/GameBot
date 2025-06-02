package gamebot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface ICommand {

	Mono<Void> execute(ChatInputInteractionEvent event);
	
}
