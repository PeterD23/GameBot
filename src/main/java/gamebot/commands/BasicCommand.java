package gamebot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.GameBot;
import reactor.core.publisher.Mono;

public class BasicCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	private TopLevelMessageComponent component;
	private boolean ephemeral = false;
	private String desc;
	
	public BasicCommand(String command, String description, TopLevelMessageComponent component) {
		this.component = component;
		this.desc = "**/"+command+"**: "+description; 
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();
		
		ApplicationCommandRequest basicRequest = ApplicationCommandRequest.builder().name(command)
				.description(description)
				.build();
		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, basicRequest).subscribe();
	}
	
	public BasicCommand ephemeral() {
		ephemeral = true;
		return this;
	}
	
	public String desc() {
		return desc;
	}
	
	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		String mention = event.getInteraction().getMember().get().getMention();
		// If top level component is TextDisplay, replace any @mention with the interactee
		if(component instanceof TextDisplay) {
			String text = component.getData().content().get();
			component = TextDisplay.of(text.replace("@member", mention));
		}
		return event.reply().withComponents(component).withEphemeral(ephemeral);
	}

}
