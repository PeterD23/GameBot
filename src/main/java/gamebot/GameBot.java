package gamebot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import meetup.MeetupEventManager;
import onlineevent.EventManager;
import reactor.core.publisher.Mono;

public class GameBot {

	public static GatewayDiscordClient gateway;

	private RoleChannelManagementListener roleListener;
	private UserListener moderationListener;
	private IntervalListener intervalListener;

	private long SERVER_ID = 731597823640076319L;
	
	public static void main(String[] args) {
		GameBot bot = new GameBot();
		bot.init(args);
	}

	public void init(String[] args) {
		if (args.length == 0)
			throw new IllegalArgumentException("Please enter a client key.");
		roleListener = new RoleChannelManagementListener();
		moderationListener = new UserListener();
		intervalListener = new IntervalListener();

		DiscordClient client = DiscordClient.create(args[0]);
		gateway = client.login().block();
		buildReadyEvent();
		buildMemberJoinEvent();
		buildMessageCreateEvent();
		buildMessageUpdateEvent();
		buildReactionAddEvent();
		buildReactionRemoveEvent();
		MeetupEventManager.init();
		EventManager.init();
		buildInterval();
		SpotifyHelpers.init(args[1], args[2]);
		
		gateway.onDisconnect().block();
	}

	private void buildReadyEvent() {
		gateway.on(ReadyEvent.class, ready -> {
			roleListener.onReady(ready);
			moderationListener.onReady(ready);
			intervalListener.onReady(ready);
			return Mono.empty();
		}).subscribe();
	}

	private void buildMemberJoinEvent() {
		gateway.on(MemberJoinEvent.class, event -> Mono.fromRunnable(() -> 
		moderationListener.newUser(event)).onErrorResume(t -> Mono.empty()))
				.subscribe();
	}

	private void buildMessageCreateEvent() {
		gateway.on(MessageCreateEvent.class, event -> Mono.fromRunnable(() -> {
			roleListener.onMessage(event);
			moderationListener.onMessage(event);
			intervalListener.onMessage(event);
		})).subscribe();
	}

	private void buildMessageUpdateEvent() {
		gateway.on(MessageUpdateEvent.class, event -> Mono.fromRunnable(() -> {
			moderationListener.onEdit(event);
		})).subscribe();
	}

	private void buildReactionAddEvent() {
		gateway.on(ReactionAddEvent.class, event -> Mono.fromRunnable(() -> {
			roleListener.onReact(event);
			moderationListener.onReact(event);
		})).subscribe();
	}

	private void buildReactionRemoveEvent() {
		gateway.on(ReactionRemoveEvent.class, event -> Mono.fromRunnable(() -> {
			roleListener.onUnreact(event);
			moderationListener.onUnreact(event);
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
