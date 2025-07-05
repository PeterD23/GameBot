package trustsystem;

import java.util.ArrayList;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import gamebot.Utils;

public class ReportedMessage {

	public String messageContent;
	public ArrayList<String> imageUrl = new ArrayList<>();
	public String channelId;
	public String messageId;
	public ArrayList<String> reasons = new ArrayList<>();
	
	public ReportedMessage() {}
	
	public ReportedMessage(Message message) {
		this.messageContent = Optional.ofNullable(message.getContent()).filter(s -> !s.isEmpty()).orElse(Utils.getContentFromComponents(message.getComponents()));
		this.channelId = message.getChannelId().asString();
		this.messageId = message.getId().asString();
		for(Attachment attachment : message.getAttachments()) {
			imageUrl.add(attachment.getUrl());
		}
	}

	@JsonIgnore
	public String contentOrImages() {
		String data = "";
		if(!messageContent.isEmpty())
			data += "`"+messageContent+"`\n";
		if(imageUrl.size() > 0);
			data += String.join("\n", imageUrl);
		return data;
	}
	
	@JsonIgnore
	public ArrayList<String> reasons(){
		return reasons;
	}
	
	@JsonIgnore
	public String getContent() {
		return messageContent;
	}

	@JsonIgnore
	public Snowflake getChannel() {
		return Snowflake.of(channelId);
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(messageId);
	}
	
	@JsonIgnore
	public void combineIfEquals(ReportedMessage message) {
		if(messageId.equals(message.messageId)) {
			reasons = Utils.combineDistinct(reasons, message.reasons());
		}
	}
	
	@Override
	public boolean equals(Object o) {
		ReportedMessage message = (ReportedMessage) o;
		return messageId.equals(message.messageId);
	}
}
