package gamebot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import meetup.MeetupEventManager;
import onlineevent.EventManager;
import reactor.core.publisher.Mono;

public class GameBot {

	public static DiscordClient CLIENT;

	private RoleChannelManagementListener roleListener;
	private ModerationListener moderationListener;
	private IntervalListener intervalListener;

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
			intervalListener = new IntervalListener();

			CLIENT = new DiscordClientBuilder(args[0]).build();
			buildReadyEvent();
			buildMemberJoinEvent();
			buildMessageCreateEvent();
			buildReactionAddEvent();
			buildReactionRemoveEvent();
			MeetupEventManager.init();
			EventManager.init();
			buildInterval();
			SpotifyHelpers.init(args[1], args[2]);

			CLIENT.login().block();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void buildReadyEvent() {
		CLIENT.getEventDispatcher().on(ReadyEvent.class).flatMap(ready -> {
			roleListener.onReady(ready);
			moderationListener.onReady(ready);
			intervalListener.onReady(ready);
			return Mono.empty();
		}).subscribe();
	}

	private void buildMemberJoinEvent() {
		CLIENT.getEventDispatcher().on(MemberJoinEvent.class).flatMap(
				event -> Mono.fromRunnable(() -> moderationListener.newUser(event)).onErrorResume(t -> Mono.empty()))
				.subscribe();
	}

	private void buildMessageCreateEvent() {
		CLIENT.getEventDispatcher().on(MessageCreateEvent.class).flatMap(event -> Mono.fromRunnable(() -> {
			roleListener.onMessage(event);
			moderationListener.onMessage(event);
			intervalListener.onMessage(event);
		}).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}

	private void buildReactionAddEvent() {
		CLIENT.getEventDispatcher().on(ReactionAddEvent.class).flatMap(event -> Mono.fromRunnable(() -> {
			roleListener.onReact(event);
			moderationListener.onReact(event);
		}).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}

	private void buildReactionRemoveEvent() {
		CLIENT.getEventDispatcher().on(ReactionRemoveEvent.class).flatMap(event -> Mono.fromRunnable(() -> {
			roleListener.onUnreact(event);
			moderationListener.onUnreact(event);
		}).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		})).subscribe();
	}

	private void buildInterval() {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			try {
				intervalListener.tick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
}
