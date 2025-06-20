package gamebot.commands;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import gamebot.ChannelLogger;
import gamebot.GameBot;
import meetup.selenium.MeetupLinker;
import meetup.selenium.SeleniumDriver;
import reactor.core.publisher.Mono;

public class LinkMeetupCommand implements ISlashCommand {

	private long guildId = GameBot.SERVER;
	public static LinkMeetupCommand command;
	private long MEETUP_VERIFIED = 902260032945651774L;
	private long EVG_LOGO_ID = 1277914506340732949L;
	private ReactionEmoji evgLogo = ReactionEmoji.custom(Snowflake.of(EVG_LOGO_ID), "evg", false);

	public static LinkMeetupCommand get() {
		if (command == null) {
			command = new LinkMeetupCommand();
		}
		return command;
	}

	public String desc() {
		return "**/link-meetup** Request me to verify you on Meetup so you can be Meetup Verified."; 
	}
	
	public LinkMeetupCommand() {
		ApplicationCommandRequest linkMeetupRequest = ApplicationCommandRequest.builder().name("link-meetup")
				.description("Ask the bot to link your Discord to your Meetup account.!").build();
		
		GatewayDiscordClient client = GameBot.gateway;
		client.getRestClient().getApplicationId()
			.flatMap(applicationId -> 
				client.getRestClient().getApplicationService()
					.createGuildApplicationCommand(applicationId, guildId, linkMeetupRequest))
			.subscribe();
	}

	private List<TopLevelMessageComponent> constructMessage(String code, String message, Color color) {
		return Arrays.asList(Container.of(color, TextDisplay.of(message), Separator.of(),
				TextDisplay.of("Step 1. Click the Button on the left to view my Meetup Profile."), Separator.of(),
				TextDisplay.of("Step 2. Send me a message with **just** this code: **" + code + "**"), Separator.of(),
				TextDisplay.of("Step 3. Click the button on the right and I'll check if you've done that."),
				Separator.of(), ActionRow.of(Button.link("https://www.meetup.com/members/343387847", evgLogo),
						Button.success("check-code", "I'm Ready"))));
	}

	@Override
	public Mono<Void> submitCommand(ChatInputInteractionEvent event) {
		String title = "Hi! You've requested to link your Meetup account to your Discord! Here's what to do:";
		Member member = event.getInteraction().getMember().get();
		long userId = member.getId().asLong();
		MeetupLinker.queueUser(userId, RandomStringUtils.randomAlphanumeric(5));
		if (MeetupLinker.isQueued(userId)) {
			String code = MeetupLinker.getUsersCode(userId);
			return event.deferReply().withEphemeral(true).then(Mono.fromRunnable(() -> {
				// Register a listener for the button
				GameBot.createTempInteraction(ButtonInteractionEvent.class, click -> onButtonClick(click),
						Duration.ofMinutes(5));
			})).then(event.editReply().withComponentsOrNull(constructMessage(code, title, Color.GREEN)).then());
		}
		return event.reply("You appear to already be Meetup verified!").withEphemeral(true);
	}

	private Mono<Void> onButtonClick(ButtonInteractionEvent event) {
		if (SeleniumDriver.getInstance().isLocked()) {
			ChannelLogger.logMessageWarning("User tried to verify identity but browser is currently locked");
			return event.reply(
					"The browser is currently busy at the moment, try again in a short while! Can't get the staff these days...")
					.withEphemeral(true);
		}
		if (event.getCustomId().equals("check-code")) {	
			Member member = event.getInteraction().getMember().get();
			long userId = member.getId().asLong();

			String code = MeetupLinker.getUsersCode(userId);
			return event.deferEdit().then(Mono.fromCallable(() -> {
                ChannelLogger.logMessageInfo("Generating a new button listener for Link-Meetup");
                return SeleniumDriver.getInstance().checkCode(code);
            })
            .flatMap(meetupId -> {
                if (meetupId == 0L) {
                    ChannelLogger.logMessageWarning("Failed to verify Meetup ID");
                    return Mono.fromCallable(() -> constructMessage(code, "Something went wrong. I wasn't able to get your Meetup ID.", Color.RED))
                    		.flatMap(components -> event.editReply().withComponentsOrNull(components))
                    		.then();
                }
                return member.addRole(Snowflake.of(MEETUP_VERIFIED))
                        .then(Mono.fromRunnable(() -> MeetupLinker.linkUserToMeetup(userId, meetupId)))
                        .then(event.deleteReply())
                        .then(event.getInteraction().getChannel())
                        .ofType(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage("Congrats "+ member.getMention() +", you're officially Meetup Verified™! Have a role for your efforts!"))
                        .then();
            }));
			
		}
		return Mono.empty();
	}
}
