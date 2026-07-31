package gamebot.listeners;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused") // Defaults are optional implementations
public interface IListener {

	// On Ready is mandatory, others are optional
	public Mono<?> onReady(GuildCreateEvent event);
	
	default public Mono<?> onMessage(MessageCreateEvent event){
		return Mono.empty();
	}
		
	default public Mono<?> onEdit(MessageUpdateEvent event) {
		return Mono.empty();
	}
	
	default public Mono<?> onMemberJoin(MemberJoinEvent event) {
		return Mono.empty();
	};
	
	default public Mono<?> onCommand(ChatInputInteractionEvent event) {
		return Mono.empty();
	};
	
	default public Mono<?> onCommand(ModalSubmitInteractionEvent event) {
		return Mono.empty();
	}

	default public Mono<?> onCommand(ButtonInteractionEvent event) {
		return Mono.empty();
	}
	
	default public Mono<?> onMessageInteraction(MessageInteractionEvent event){
		return Mono.empty();
	}
	
}
