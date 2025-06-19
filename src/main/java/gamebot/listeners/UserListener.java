package gamebot.listeners;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Random;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.Separator.SpacingSize;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import gamebot.ChannelLogger;
import gamebot.CoreHelpers;
import gamebot.Utils;
import gamebot.commands.BasicCommand;
import gamebot.commands.BirthdayCommand;
import gamebot.commands.ISlashCommand;
import gamebot.commands.LinkMeetupCommand;
import gamebot.commands.PollCommand;
import gamebot.commands.SubscribeCommand;
import gamebot.commands.WatchCommand;
import meetup.selenium.MeetupLinker;
import reactor.core.publisher.Mono;

public class UserListener extends CoreHelpers implements IListener {

	private HashMap<String, ISlashCommand> commands = new HashMap<>();

	// Roles
	private long INTRODUCTIONS = 732247266173124648L;
	private long VERIFIED = 732253932482723881L;

	public Mono<?> onReady(GuildCreateEvent event) {
		return init(event).then(initialiseCommands()).then(Mono.fromRunnable(() -> MeetupLinker.readVerified()));
	}

	public Mono<?> onMessage(MessageCreateEvent event) {
		if (Utils.isTestingMode() || !event.getMember().isPresent())
			return Mono.empty();

		Message message = event.getMessage();
		return message.getChannel().ofType(MessageChannel.class).flatMap(channel -> {
			if (!event.getMember().isPresent()) {
				return Mono.empty();
			}

			Member usr = event.getMember().get();
			if (usr.isBot())
				return Mono.empty();

			if (!hasRole(usr, VERIFIED)) {
				return checkVerify(message, usr, false);
			}

			String msg = message.getContent();
			if (channel.getId().asLong() == MUSIC && new Random().nextInt(6) == 5
					&& msg.startsWith("https://open.spotify.com/track/")) {
				return sendMessage(MUSIC, "https://c.tenor.com/1S9zA-EMU4YAAAAC/stay-out.gif");
			}

			return Mono.empty();
		});
	}

	public Mono<?> onEdit(MessageUpdateEvent event) {
		if (Utils.isTestingMode())
			return Mono.empty();

		return event.getMessage().flatMap(message -> message.getAuthorAsMember().flatMap(usr -> {
			if (usr.isBot()) {
				return Mono.empty();
			}
			return hasRole(usr, VERIFIED) ? Mono.empty() : checkVerify(message, usr, true);
		}));
	}

	public Mono<Void> onMemberJoin(MemberJoinEvent event) {
		if (Utils.isTestingMode())
			return Mono.empty();
		Member usr = event.getMember();
		return introduceYourself(usr, INTRODUCTIONS)
				.then(ChannelLogger.logMessageInfo("Somebody new joined the server! User ID " + usr.getId().asLong()));
	}

	// Slash Commands
	public Mono<?> onCommand(ChatInputInteractionEvent event) {
		String command = event.getCommandName();
		if (Utils.isTestingMode() && !command.equals("test")) {
			return Mono.empty();
		}
		if (commands.get(command) != null) {
			return commands.get(command).submitCommand(event);
		}
		return Mono.empty();
	}

	public Mono<?> onCommand(ModalSubmitInteractionEvent event) {
		return commands.get(event.getCustomId()).onModalSubmit(event);
	}

	private Mono<Void> initialiseCommands() {
		return Mono.fromRunnable(() -> {
			commands.put("hello", hello());
			commands.put("about", about());
			commands.put("link-meetup", LinkMeetupCommand.get());
			commands.put("poll", PollCommand.get());
			commands.put("watch", WatchCommand.get());
			commands.put("birthday", BirthdayCommand.get());
			commands.put("help", help());
			commands.put("subscribe", SubscribeCommand.get());
		});
	}

	private BasicCommand help() {
		Container container = Container.of(
				TextDisplay.of("Hi there! Here is a list of the slash commands that are currently implemented:"),
				Separator.of(SpacingSize.LARGE));
		for (ISlashCommand command : commands.values()) {
			container = container.withAddedComponents(TextDisplay.of(command.desc()), Separator.of());
		}
		container = container.withAddedComponent(
				TextDisplay.of("If you have any feature requests, please let " + mentionMe() + " know! ^^"));
		return new BasicCommand("help", "Display a list of commands", container).ephemeral();
	}

	private BasicCommand about() {
		TextDisplay text = TextDisplay.of(
				"Hi! I'm Game Bot, and I help out managing peoples roles on the server!\n My inner workings were initially created by"
						+ mentionMe() + "! However, I'm also supported by these lovely people:\n"
						+ mentionUsersWithRole(933380677892730880L)
						+ "So, if at any point I stop working, please give any of them a kick!");
		return new BasicCommand("about", "Who is Game Bot?", text);
	}

	private BasicCommand hello() {
		return new BasicCommand("hello", "Say hi to Game Bot!",
				TextDisplay.of("Hello @member! " + Utils.getARandomGreeting()));
	}

	private Mono<Void> checkVerify(Message message, Member member, boolean isEdit) {
		int count = message.getContent().split("\\s").length;
		if (count >= 30) {
			return member.addRole(Snowflake.of(VERIFIED))
					.then(message.addReaction(ReactionEmoji.codepoints("U+1F525")))
					.then(sendReply(message, member.getMention()
							+ " Cool! I've verified you. You now have access to the rest of the server! You can subscribe to extra channels using the /subscribe command, and use /help if you want to see what other things I can do!"));
		} else if (!isEdit) {
			return sendReply(message,
					"Hi, unfortunately that introduction wasn't quite long enough. Remember, 30 words or more! You can edit your message instead of making a new one since I have super secret sensing powers!");
		}
		return Mono.empty(); // No-op (Message edit doesn't hit >30 words)
	}

	private Mono<Message> introduceYourself(Member member, long channelId) {
		Instant userCreationTime = member.getId().getTimestamp();
		long accountAge = ChronoUnit.DAYS.between(userCreationTime, Instant.now());
		if (hasRole(member, VERIFIED)) {
			return sendMessage(channelId, "Hello " + member.getMention() + "! " + Utils.getARandomGreeting());
		} else if (accountAge > 14) {
			return sendMessage(channelId, "Hello " + member.getMention()
					+ "! Welcome to the server! You'll need to get verified before you can post in other channels, so type a short (30 words or more) introduction about yourself, and your meetup name.");
		} else {
			return sendMessage(channelId, "Hello " + member.getMention()
					+ "! Welcome to the server! You appear to be less than 14 days old so I am unable to automatically verify you. Please sit tight and a member of our staff will be with you shortly.");
		}
	}
}
