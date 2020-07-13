package gamebot;

import discord4j.core.event.domain.message.MessageCreateEvent;

public interface UserCommand {

	void execute(MessageCreateEvent event, String command);
}
