package gamebot;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.gateway.intent.IntentSet;
import gamebot.listeners.IListener;
import gamebot.listeners.IntervalListener;
import gamebot.listeners.RoleChannelManagementListener;
import gamebot.listeners.UserListener;
import meetup.MeetupEventManager;
import onlineevent.EventManager;
import reactor.core.publisher.Mono;

public class GameBot {

	public static GatewayDiscordClient gateway;
	private ArrayList<IListener> listeners = new ArrayList<>();
	private long SERVER = 731597823640076319L;

	public static void main(String[] args) {
		new GameBot().init(args);
	}

	public void init(String[] args) {
		if (args.length == 0)
			throw new IllegalArgumentException("Please enter a client key.");

		IntervalListener interval = new IntervalListener();
		listeners.add(new RoleChannelManagementListener());
		listeners.add(new UserListener());
		listeners.add(interval);

		DiscordClient client = DiscordClient.create(args[0]);
		gateway = client.gateway().setEnabledIntents(IntentSet.all()).login().block();
		buildReadyEvent();
		buildMemberJoinEvent();
		buildMessageCreateEvent();
		buildMessageUpdateEvent();
		buildReactionAddEvent();
		buildReactionRemoveEvent();
		MeetupEventManager.init();
		EventManager.init();
		buildInterval(interval);
		SpotifyHelpers.init(args[1], args[2]);

		gateway.onDisconnect().block();
	}

	private void buildReadyEvent() {
		gateway.on(ReadyEvent.class, ready -> {
			Guild g = gateway.getGuildById(Snowflake.of(SERVER)).block();
			ChannelLogger.init(g);
			listeners.forEach(element -> element.onReady(ready));
			return Mono.empty();
		}).subscribe();
	}

	private void buildMemberJoinEvent() {
		gateway.on(MemberJoinEvent.class,
				event -> Mono.fromRunnable(() -> listeners.forEach(element -> element.onMemberJoin(event)))
						.onErrorResume(t -> Mono.empty()))
				.subscribe();
	}

	private void buildMessageCreateEvent() {
		gateway.on(MessageCreateEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onMessage(event));
		})).onErrorResume(t -> { 
			ChannelLogger.logHighPriorityMessage("MessageCreateEvent error occurred.", t);
			return Mono.empty();
		}).subscribe();
	}

	private void buildMessageUpdateEvent() {
		gateway.on(MessageUpdateEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onEdit(event));
		})).onErrorResume(t -> Mono.empty()).subscribe();
	}

	private void buildReactionAddEvent() {
		gateway.on(ReactionAddEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onReact(event));
		})).onErrorResume(t -> Mono.empty()).subscribe();
	}

	private void buildReactionRemoveEvent() {
		gateway.on(ReactionRemoveEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onUnreact(event));
		})).onErrorResume(t -> Mono.empty()).subscribe();
	}

	private void buildInterval(IntervalListener interval) {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			try {
				interval.tick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
}
