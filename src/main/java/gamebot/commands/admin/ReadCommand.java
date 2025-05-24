package gamebot.commands.admin;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.MediaGallery;
import discord4j.core.object.component.MediaGalleryItem;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.UnfurledMediaItem;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import gamebot.commands.ISlashCommand;
import reactor.core.publisher.Mono;

public class ReadCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	public static ReadCommand command;

	public static ReadCommand get() {
		if (command == null) {
			command = new ReadCommand();
		}
		return command;
	}

	protected static Mono<Void> sendMessage(Snowflake channelId, Container component) {
		return GameBot.gateway.getChannelById(channelId).flatMap(first -> {
			TextChannel channel = (TextChannel) first;
			return channel.createMessage().withComponents(component);
		}).then();
	}

	public ReadCommand() {
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();

		ApplicationCommandRequest readRequest = ApplicationCommandRequest.builder().name("read")
				.description("Read contents for a file")
				.addOption(ApplicationCommandOptionData.builder().name("filename").description("File name to read")
						.type(ApplicationCommandOption.Type.STRING.getValue()).required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("cc_channel").description("CC to this channel")
						.type(ApplicationCommandOption.Type.CHANNEL.getValue()).required(false).build())
				.build();

		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, readRequest).subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		String fileName = event.getOptionAsString("filename").get().trim();
		Optional<Channel> cc = event.getOptionAsChannel("cc_channel").blockOptional();
		try {
			ArrayList<String> lines = new ArrayList<>(
					FileUtils.readLines(new File(fileName), Charset.defaultCharset()));
			return event.deferReply().withEphemeral(true).then(constructMessageFromFile(lines)).flatMap(component -> {
				if (cc.isPresent()) {
					TextChannel channel = (TextChannel) cc.get();
					ChannelLogger.logMessageInfo("Valid channel present, CC'ing to " + channel.getName());
					return sendMessage(channel.getId(), component).then(event.editReply().withComponents(component));
				}
				return event.editReply().withComponents(component);
			}).doOnError(err -> event.editReply("Outer loop broken")).then();
		} catch (Exception e) {
			ChannelLogger.logMessageError("Unable to find file '"+fileName+"' due to ", e);
			return event.reply("Cannot load the file: "+e.getMessage()).withEphemeral(true);
		}
	}

	private Mono<Container> constructMessageFromFile(ArrayList<String> lines) {
		Container container = Container.of();
		String temp = "";
		for (String line : lines) {
			if (line.equals("---")) {
				container = container.withAddedComponents(TextDisplay.of(temp), Separator.of());
				temp = "";
			} else if (line.startsWith("https://")) {
				container = container.withAddedComponents(TextDisplay.of(temp), constructMediaGallery(line));
				temp = "";
			} else {
				temp += line + "\n";
			}
		}
		return Mono.just(container);
	}

	private MediaGallery constructMediaGallery(String imageLine) {
		String[] split = imageLine.split(",");
		ArrayList<MediaGalleryItem> gallery = new ArrayList<>();
		for (String url : split) {
			gallery.add(MediaGalleryItem.of(UnfurledMediaItem.of(url)));
		}
		return MediaGallery.of(gallery);
	}

}
