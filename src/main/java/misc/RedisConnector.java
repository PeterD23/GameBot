package misc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import gamebot.ChannelLogger;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI.Builder;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import misc.MessageCache.PartialMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class RedisConnector {

	public static RedisClient redis;
	private static Logger log = Loggers.getLogger("redis");

	private static ObjectWriter ow;
	private static ObjectMapper om;

	public static RedisClient getRedisClient() {
		if (redis == null) {
			redis = RedisClient
					.create(Builder.redis("192.168.0.225", 6379).withAuthentication("default", "gamebot").build());
			ow = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).writer()
					.withDefaultPrettyPrinter();
			om = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		}
		return redis;
	}

	@SuppressWarnings("resource")
	public static RedisReactiveCommands<String, String> reactiveConnect() {
		StatefulRedisConnection<String, String> connection = redis.connect();
		return connection.reactive();
	}

	public static Mono<Void> writeFullMessageCache(HashMap<String, ArrayList<PartialMessage>> map) {
		RedisReactiveCommands<String, String> reactiveCommands = reactiveConnect();
		ObjectWriter ow = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).writer()
				.withDefaultPrettyPrinter();
		return ChannelLogger.logMessageInfo("Writing Message Cache of size " + map.size() + " to Redis...")
				.then(Mono.just(System.currentTimeMillis()))
				.flatMap(start -> Flux.fromIterable(map.entrySet()).flatMap(entry -> {
					try {
						String toJson = ow.writeValueAsString(entry.getValue());
						return reactiveCommands.hset("gamebot:messagecache", entry.getKey(), toJson);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						return Mono.empty();
					}
				}).then(ChannelLogger.logMessageInfo(
						"Write complete! Took " + (System.currentTimeMillis() - start) + " milliseconds")))
				.then();
	}

	private static <T> Mono<String> append(String source, T object) {
		CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class,
				object.getClass());
		try {
			List<T> list = om.readValue(source, typeReference);
			list.add(object);
			return Mono.just(ow.writeValueAsString(list));
		} catch (Exception e) {
			return ChannelLogger.logMessageError("Error in appending", e).then(Mono.empty());
		}
	}

	private static <T> Mono<Void> putInitialValue(String key, String field, T object) {
		List<T> list = new ArrayList<>();
		list.add(object);
		try {
			return reactiveConnect().hset(key, field, ow.writeValueAsString(list)).then();
		} catch (Exception e) {
			return ChannelLogger.logMessageError("Error in putting initial value", e);
		}
	}

	// Read a hash in the type of T and convert the json string into a list
	public static <T> Mono<List<T>> readList(String key, String field, Class<T> type) {
		return readValue(key, field).hasElement().flatMap(has -> has ? readValue(key, field).map(entry -> {
			CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, type);
			try {
				return om.readValue(entry, typeReference);
			} catch (Exception e) {
				e.printStackTrace();
				return new ArrayList<>();

			}
		}) : Mono.just(new ArrayList<>()));
	}

	public static Mono<String> readValue(String key, String field) {
		return reactiveConnect().hget(key, field);
	}

	public static <T> Mono<Optional<T>> readValue(String key, String field, Class<T> type) {
		return reactiveConnect().hget(key, field).flatMap(entry -> {
			try {
				return Mono.just(Optional.ofNullable(om.readValue(entry, type)));
			} catch (Exception e) {
				return Mono.fromRunnable(() -> e.printStackTrace()).then(Mono.just(Optional.empty()));
			}
		});
	}

	public static <T> Mono<Void> appendEntry(String key, String field, T object) {
		return readValue(key, field).hasElement()
				.flatMap(has -> has
						? readValue(key, field).flatMap(
								data -> append(data, object).flatMap(list -> reactiveConnect().hset(key, field, list)))
						: putInitialValue(key, field, object))
				.then();
	}

	/**
	 * Java Generics are truly the wild west of programming, good fucking lord... So
	 * this merges two objects together if one already exists in the cache then
	 * pushes the result. Think of it like git pull branch, it pulls the latest data
	 * from the cache, merges them then pushes. This should *hopefully* avoid
	 * asynchronous data loss from race conditions
	 */
	public static <T extends Mergeable<T>> Mono<Void> mergeObject(String key, String field, Class<T> type, T object) {
		return readValue(key, field, type).defaultIfEmpty(Optional.empty()).map(value -> {
			return value.isPresent() ? object.merge(value.get()) : object;
		}).flatMap(merged -> {
			try {
				return reactiveConnect().hset(key, field, ow.writeValueAsString(merged));
			} catch (Exception e) {
				return Mono.fromRunnable(() -> e.printStackTrace());
			}
		}).then();
	}

	public static Mono<Void> cacheEntry(String key, String field, String value) {
		return reactiveConnect().hset(key, field, value).then();
	}

	public static Mono<Void> cacheEntry(String key, Pair<String, List<String>> pair) {
		return reactiveConnect().hset(key, pair.getLeft(), Utils.listToSSVString(pair.getRight())).then();
	}

	public static Mono<Void> deleteEntry(String key, String... fields) {
		if(fields.length == 0) {
			return Mono.empty();
		}
		return reactiveConnect().hdel(key, fields).then();
	}

	public static Mono<HashMap<String, String>> cacheFile(File file, String key) {
		log.info("Caching key " + key);
		return Mono.just(file).map(f -> {
			try {
				return FileUtils.readLines(f, Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
				Exceptions.propagate(e);
				return new ArrayList<String>();
			}
		}).map(RedisConnector::transformSSVListToValueMap).flatMap(map -> {
			return reactiveConnect().hset(key, map).then(Mono.fromCallable(() -> transformMaptoHashMapOfN(map)));
		});
	}

	private static HashMap<String, String> transformMaptoHashMapOfN(Map<String, String> map) {
		HashMap<String, String> hash = new HashMap<>();
		map.forEach((k, v) -> hash.put(k, v));
		return hash;
	}

	// Transform SSV values on a 1 to n basis, skipping the key X e.g "X Y Z" -> (Y,
	// Z)
	private static Map<String, String> transformSSVListToValueMap(List<String> list) {
		return list.stream().map(item -> item.split(" ")).collect(Collectors.toMap(v -> v[0],
				v -> Utils.listToSSVString(Arrays.stream(v).skip(1).collect(Collectors.toList()))));
	}

}
