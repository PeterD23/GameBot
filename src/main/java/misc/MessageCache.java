package misc;

import static gamebot.EvgIds.GENERAL_CATEGORY;
import static gamebot.EvgIds.GENRES_CATEGORY;
import static gamebot.EvgIds.TOPICS_CATEGORY;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import gamebot.ChannelLogger;
import reactor.core.publisher.Mono;
import reactor.util.Loggers;

public class MessageCache {

	public static class PartialMessage {
		public String content;
		public String timestamp;

		public PartialMessage(){}
		
		public PartialMessage(Message message) {
			content = message.getContent();
			timestamp = message.getTimestamp().toString();
		}
	}

	private static String key = "gamebot:messagecache";
	private static HashMap<String, ArrayList<PartialMessage>> cache = new HashMap<>();

	public static Mono<Void> cacheUsers(Guild guild) { 
		return guild.getChannels()
				.ofType(TextChannel.class)
				.filter(channel -> {
					Optional<Snowflake> category = channel.getCategoryId();
					return category.isPresent() && (category.get().equals(GENRES_CATEGORY.snow()) || category.get().equals(GENERAL_CATEGORY.snow()) || category.get().equals(TOPICS_CATEGORY.snow()));
				})
				.flatMap(channel -> {
					Instant start = Instant.now();
					return channel.getMessagesBefore(Snowflake.of(Instant.now()))
							.flatMap(message -> {
									if (message.getAuthor().isPresent() && !message.getContent().isEmpty()) {
										String id = message.getAuthor().get().getId().asString();
										cache.computeIfAbsent(id, v -> new ArrayList<>()).add(new PartialMessage(message));
										return Mono.just(1);
									}
									return ChannelLogger.logMessageTrace(message.getId().asString() +" has no author");
									})
								.onErrorResume(t -> ChannelLogger.logMessageError("Failed to add Message to Cache: ", t)
								)
							.log(Loggers.getLogger("MessageCache"))
							.count()
							.flatMap(count -> ChannelLogger.logMessageInfo("Cached all "+count+" channel messages for "+channel.getName()+" in "+
									ChronoUnit.SECONDS.between(start, Instant.now())+" seconds"));
				})
				.collectList()
				.flatMap(list -> RedisConnector.writeFullMessageCache(cache));
	}
	
	public static Mono<ArrayList<PartialMessage>> getUserFromCache(String id){
		return RedisConnector.readValue(key, id).map(string -> {
			CollectionType typeReference =
				    TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, PartialMessage.class);
			ObjectMapper reader = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			try {
				return reader.readValue(string, typeReference);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}

}
