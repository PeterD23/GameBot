package gamebot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.gateway.intent.IntentSet;
import gamebot.listeners.AdminListener;
import gamebot.listeners.IListener;
import gamebot.listeners.UserListener;
import meetup.selenium.MeetupEventManager;
import reactor.core.publisher.Mono;

public class GameBot {

	public static GatewayDiscordClient gateway;
	private ArrayList<IListener> listeners = new ArrayList<>();
	public static long SERVER = 731597823640076319L;

	public static void main(String[] args) {
		new GameBot().init(args);
	}

	public void init(String[] args) {
		if (args.length == 0)
			throw new IllegalArgumentException("Please enter a client key.");

		AdminListener admin = new AdminListener();
		listeners.add(admin);
		listeners.add(new UserListener());

		DiscordClient client = DiscordClient.create(args[0]);
		gateway = client.gateway().setEnabledIntents(IntentSet.all()).login().block();
		buildReadyEvent();
		buildMemberJoinEvent();
		buildMessageCreateEvent();
		buildMessageUpdateEvent();
		buildChatInputInteractionEvent();
		MeetupEventManager.init();
		buildTimedInterval(admin);
		SpotifyHelpers.init(args[1], args[2]);

		gateway.onDisconnect().block();
	}

	public static <E extends ComponentInteractionEvent, T> void createTempInteraction(Class<E> event,
			Function<E, Publisher<T>> function, Duration timeout) {
		String callerMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
		ChannelLogger.logMessageInfo("Creating new temp listener for " + event.getTypeName() + " from " + callerMethod);
		gateway.on(event, function).timeout(timeout).onErrorResume(TimeoutException.class, ignore -> {
			ChannelLogger.logMessageWarning("Timeout: " + event.getTypeName() + " from " + callerMethod);
			return Mono.empty();
		}).then().subscribe();
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

	private void buildChatInputInteractionEvent() {
		gateway.on(ChatInputInteractionEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onCommand(event));
		})).onErrorResume(t -> Mono.empty()).subscribe();

		gateway.on(ModalSubmitInteractionEvent.class, event -> Mono.fromRunnable(() -> {
			listeners.forEach(element -> element.onCommand(event));
		})).onErrorResume(t -> Mono.empty()).subscribe();
	}

	private void buildTimedInterval(AdminListener admin) {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			try {
				admin.tick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
}
