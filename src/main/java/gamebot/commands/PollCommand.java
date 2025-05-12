package gamebot.commands;

import java.util.ArrayList;
import java.util.Arrays;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.poll.Poll;
import discord4j.core.object.entity.poll.PollAnswer;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.PollCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gamebot.GameBot;
import reactor.core.publisher.Mono;

public class PollCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	private long POLL_WATCHER = 908032897762619433L;
	private String pollWatcherMention = "";
	public static PollCommand command;

	public static PollCommand get() {
		if (command == null) {
			command = new PollCommand();
		}
		return command;
	}
	
	public String desc() {
		return "**/poll** Create a Discord poll. **expiry** is required from a dropdown list of options"; 
	}

	public PollCommand() {
		GatewayDiscordClient client = GameBot.gateway;
		long applicationId = client.getRestClient().getApplicationId().block();
		pollWatcherMention = client.getGuildById(Snowflake.of(guildId)).block()
				.getRoles()
				.filter(p -> p.getId().asLong() == POLL_WATCHER)
				.next()
				.block()
				.getMention();
		
		ArrayList<ApplicationCommandOptionChoiceData> choices = new ArrayList<>();
		choices.add(ApplicationCommandOptionChoiceData.builder().name("12 hours").value(12).build());
		choices.add(ApplicationCommandOptionChoiceData.builder().name("1 day").value(24).build());
		choices.add(ApplicationCommandOptionChoiceData.builder().name("7 days").value(168).build());
		choices.add(ApplicationCommandOptionChoiceData.builder().name("14 days").value(336).build());
		choices.add(ApplicationCommandOptionChoiceData.builder().name("1 month").value(768).build());
		
		ApplicationCommandRequest pollRequest = ApplicationCommandRequest.builder().name("poll")
				.description("Creates a poll.")
				.addOption(ApplicationCommandOptionData.builder().name("expiry").description("How many hours should the poll last?")
						.type(ApplicationCommandOption.Type.INTEGER.getValue()).required(true).choices(choices).build())
				.build();

		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(applicationId, guildId, pollRequest).subscribe();
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		int expiry = event.getOptionAsLong("expiry").get().intValue();
		InteractionPresentModalSpec.Builder modalBuilder = InteractionPresentModalSpec.builder()
				.title("Poll the audience!").customId("poll")
				.addComponent(ActionRow.of(TextInput.small("title", "Insert your question here").required()))
				.addComponent(ActionRow.of(TextInput.paragraph(expiry, "options", "Put options here, add a new line for each one")));
		 
		return event.presentModal(modalBuilder.build());
	}

	@Override
	public Mono<Void> onModalSubmit(ModalSubmitInteractionEvent event) {
		ArrayList<TextInput> components = new ArrayList<>(event.getComponents(TextInput.class));
		String question = components.get(0).getValue().orElse("Oopsie");
		String[] optionsArray = components.get(1).getValue().orElse("").split("\n");
		ArrayList<String> options = optionsArray[0].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(optionsArray));
		
		ArrayList<PollAnswer> answers = new ArrayList<>();
		for(String option : options) {
			PollAnswer answer = PollAnswer.of(option);
			answers.add(answer);
		}
		// Fallback if poll has no questions
		if(answers.isEmpty()) {
			answers.add(PollAnswer.of("Yes"));
			answers.add(PollAnswer.of("No"));
		}
		int expiry = components.get(1).getId();	
		PollCreateSpec pollCreateSpec = PollCreateSpec.builder().question(question).allowMultiselect(true).addAllAnswers(answers).duration(expiry).build();
		event.reply("Question time, folks... "+pollWatcherMention).block();
		Poll poll = event.getInteraction().getChannel().block().createPoll(pollCreateSpec).block();
		return event.getInteraction().getChannel().block().getMessageById(poll.getId()).block().pin();
	}
}
