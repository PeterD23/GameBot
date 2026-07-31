package gamebot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class BasicCommand implements ISlashCommand {

	private TopLevelMessageComponent component;
	private boolean ephemeral = false;
	private Mono<String> monoSubstitution = Mono.empty();

	private String command;
	private String description;

	public BasicCommand(String command, String description, TopLevelMessageComponent component) {
		this.component = component;
		this.command = command;
		this.description = description;
	}

	public ImmutableApplicationCommandRequest getCommandRequest() {
		return ApplicationCommandRequest.builder().name(command).description(description).build();
	}

	public BasicCommand withMono(Mono<String> data) {
		this.monoSubstitution = data;
		return this;
	}

	public BasicCommand ephemeral() {
		ephemeral = true;
		return this;
	}

	public String desc() {
		return "**/" + command + "**: " + description;
	}

	private Mono<TopLevelMessageComponent> modifyTextDisplayIfApplicable(String toReplace, String replacement) {
		if (component instanceof TextDisplay) {
			String text = component.getData().content().get();
			return Mono.just(TextDisplay.of(text.replace(toReplace, replacement)));
		}
		return Mono.just(component);
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		String mention = event.getInteraction().getMember().get().getMention();
		// If top level component is TextDisplay, replace any @mention with the
		// interactee
		return event.deferReply().withEphemeral(ephemeral).checkpoint("Start of Deferral").then(modifyTextDisplayIfApplicable("@member", mention))
				.flatMap(component -> monoSubstitution
						.flatMap(data -> modifyTextDisplayIfApplicable("@mono", data)
								.flatMap(modified -> event.editReply().withComponents(modified)))
						.checkpoint("Mono branch")
						.switchIfEmpty(event.editReply().withComponents(component)))
						.checkpoint("Empty branch")
				.then();
	}

}
