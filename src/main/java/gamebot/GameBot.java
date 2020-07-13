package gamebot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import reactor.core.publisher.Mono;

public class GameBot {

	public static DiscordClient CLIENT;

	private RoleChannelManagementListener roleListener;
	private ModerationListener moderationListener;
	
	public static void main(String[] args) {
		GameBot bot = new GameBot();
		bot.init(args);
	}
	
	public void init(String[] args) {
		try {
			if (args.length == 0)
				throw new IllegalArgumentException("Please enter a client key.");
			roleListener = new RoleChannelManagementListener();
			moderationListener = new ModerationListener();

			CLIENT = new DiscordClientBuilder(args[0]).build();
			buildReadyEvent();
			buildMemberJoinEvent();
			buildMessageCreateEvent();
			buildReactionAddEvent();
			buildReactionRemoveEvent();
			CLIENT.login().block();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void buildReadyEvent() {
		CLIENT.getEventDispatcher().on(ReadyEvent.class).flatMap(ready -> {
			roleListener.onReady(ready);
			moderationListener.onReady(ready);
			return Mono.empty();
		}).subscribe();
	}
	
	private void buildMemberJoinEvent() {
		CLIENT.getEventDispatcher().on(MemberJoinEvent.class).flatMap(event -> Mono
				.fromRunnable(() -> moderationListener.newUser(event)).onErrorResume(t -> Mono.empty()))
				.subscribe();
	}

	private void buildMessageCreateEvent() {
		CLIENT.getEventDispatcher().on(MessageCreateEvent.class).flatMap(event -> Mono.fromRunnable(() -> {
			roleListener.onMessage(event);
			moderationListener.onMessage(event);
		}).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}
	
	private void buildReactionAddEvent() {
		CLIENT.getEventDispatcher().on(ReactionAddEvent.class)
		.flatMap(event -> Mono.fromRunnable(() -> roleListener.onReact(event)).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}
	
	private void buildReactionRemoveEvent() {
		CLIENT.getEventDispatcher().on(ReactionRemoveEvent.class)
		.flatMap(event -> Mono.fromRunnable(() -> roleListener.onUnreact(event)).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}
	
}