package gamebot.listeners;

import discord4j.core.event.domain.guild.MemberJoinEvent;
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
	
	default public void onReact(ReactionAddEvent event) {
		return;
	};
	
	default public void onUnreact(ReactionRemoveEvent event) {
		return;
	};
	
}
