package gamebot;

import java.time.Duration;
import java.util.ArrayList;

import org.slf4j.LoggerFactory;

import config.BaseConfig;
import config.ConfigLoader;
import discord4j.common.store.Store;
import discord4j.common.store.legacy.LegacyStoreLayout;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.gateway.intent.IntentSet;
import discord4j.store.redis.RedisStoreService;
import gamebot.listeners.AdminListener;
import gamebot.listeners.IListener;
import gamebot.listeners.UserListener;
import io.lettuce.core.RedisClient;
import meetup.selenium.MeetupEventManager;
import misc.RedisConnector;
import misc.SpotifyHelpers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Loggers;

public class GameBot {

	public static GatewayDiscordClient gateway;
	private ArrayList<IListener> listeners = new ArrayList<>();

	public static void main(String[] args) {
		ConfigLoader.init();
		new GameBot().init();
	}

	/**
	 * Okay there is a lot going on here so I'll explain the gist: Mono.when() is an
	 * reactive operation that only completes once all Publishers have completed.
	 * when() should be used when you're acting on multiple Monos
	 * 
	 * @param args
	 */
	public void init() {
		BaseConfig base = ConfigLoader.base();
		LoggerFactory.getLogger("init").info("SLF4J bound to Logback");
		AdminListener admin = new AdminListener();
		listeners.add(admin);
		listeners.add(new UserListener());

		@SuppressWarnings("resource")
		RedisClient redis = RedisConnector.getRedisClient(base);
		// Hooks.onOperatorDebug(); Use for debugging
		DiscordClient client = DiscordClientBuilder.create(base.getDiscordKey()).build();
//			    .onClientResponse(ResponseFunction.retryWhen(
//			            RouteMatcher.any(),
//			            Retry.backoff(100, Duration.ofSeconds(2)).filter(throwable ->
//			                throwable instanceof SocketException ||
//			                throwable instanceof Errors.NativeIoException
//			            )
//			        ))
//			        .build();
		gateway = client.gateway()
				.setStore(Store.fromLayout(LegacyStoreLayout.of(RedisStoreService.builder().redisClient(redis).build())))
				.setEnabledIntents(IntentSet.all())
				.login().block();
		buildReadyEvent()
				.then(Mono.when(buildMemberJoinEvent(), 
						buildMessageCreateEvent(), 
						buildMessageInteractionEvent(), 
						buildTimedInterval(admin),
						buildMessageUpdateEvent(), 
						buildChatInputInteractionEvent(), 
						MeetupEventManager.init(), 
						SpotifyHelpers.init()))
				.then(gateway.onDisconnect()
						.then(Mono.fromRunnable(() -> redis.shutdown())))
				.block();
	}

	// Mono.when() is used on Flux.fromIterable because listeners is a list of two
	// items
	private Mono<?> buildReadyEvent() {
		return gateway.on(GuildCreateEvent.class).next().flatMap(ready -> {
			return ChannelLogger.init(ready.getGuild())
					.then(Mono.when(Flux.fromIterable(listeners).flatMap(element -> element.onReady(ready)))).doOnSuccess(v -> ChannelLogger.logReady());
		}).onErrorResume(t -> {
			t.printStackTrace();
			return Mono.empty();
		}).log(Loggers.getLogger("ReadyEvent"));
	}

	private Flux<?> buildMemberJoinEvent() {
		return gateway.on(MemberJoinEvent.class,
				event -> Mono.when(Flux.fromIterable(listeners).flatMap(element -> element.onMemberJoin(event))))
						.onErrorResume(t -> ChannelLogger.logHighPriorityMessage("MemberJoinEvent error occurred.", t));
	}

	private Flux<?> buildMessageCreateEvent() {
		return gateway
			.on(MessageCreateEvent.class,
				event -> Mono.when(Flux.fromIterable(listeners).flatMap(element -> element.onMessage(event))))
					.onErrorResume(t -> ChannelLogger.logHighPriorityMessage("MessageCreateEvent error occurred.", t));
	}

	private Flux<?> buildMessageUpdateEvent() {
		return gateway
				.on(MessageUpdateEvent.class,
						event -> Mono.when(Flux.fromIterable(listeners).flatMap(element -> element.onEdit(event))))
				.onErrorResume(t -> ChannelLogger.logHighPriorityMessage("MessageUpdateEvent error occurred.", t));
	}

	private Mono<?> buildChatInputInteractionEvent() {
		return Mono.when(
				gateway.on(ChatInputInteractionEvent.class,
								event -> Mono.when(
										Flux.fromIterable(listeners).flatMap(element -> element.onCommand(event))))
						.onErrorResume(t -> ChannelLogger.logMessageError("ChatInputInteractionEvent threw an error:", t)), 
						gateway.on(ModalSubmitInteractionEvent.class,
										event -> Mono.when(Flux.fromIterable(listeners)
												.flatMap(element -> element.onCommand(event))))
								.onErrorResume(t -> ChannelLogger.logMessageError("ModalSubmitInteractionEvent threw an error:", t)),
						gateway.on(ButtonInteractionEvent.class,
								event -> Mono.when(
										Flux.fromIterable(listeners).flatMap(element -> element.onCommand(event))))
								.onErrorResume(t -> ChannelLogger.logMessageError("ButtonInteractionEvent threw an error:", t)));
	}
	
	private Flux<?> buildMessageInteractionEvent(){
		return gateway.on(MessageInteractionEvent.class, event -> Mono.when(
				Flux.fromIterable(listeners).flatMap(element -> element.onMessageInteraction(event))))
				.onErrorResume(t -> ChannelLogger.logMessageError("MessageInteractionEvent threw an error:", t));
	}

	private Flux<?> buildTimedInterval(AdminListener admin) {
		return Flux.interval(Duration.ofMinutes(1)).flatMap(a -> admin.tick().then()
				.onErrorResume(t -> ChannelLogger.logMessageError("Timed interval encountered an error.", t)));
	}
}
