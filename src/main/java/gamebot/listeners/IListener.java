package gamebot.listeners;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;

@SuppressWarnings("unused") // Defaults are optional implementations
public interface IListener {

	// On Ready and On Message are mandatory, others are optional
	public void onReady(ReadyEvent event);
	
	public void onMessage(MessageCreateEvent event);
		
	default public void onEdit(MessageUpdateEvent event) {
		return;
	}
	
	default public void onMemberJoin(MemberJoinEvent event) {
		return;
	};
	
	default public void onCommand(ChatInputInteractionEvent event) {
		return;
	};
	
	default public void onCommand(ModalSubmitInteractionEvent event) {
		return;
	};
	
}
