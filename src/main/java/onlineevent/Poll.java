package onlineevent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;

public class Poll {
	
	private String[] emojis = new String[] { ":one:", ":two:", ":three:", ":four:", ":five:", ":six:" + ":seven:", ":eight:", ":nine:" };
	private String[] unicodeEmojis = new String[] { "\u0031\u20E3", "\u0032\u20E3","\u0033\u20E3","\u0034\u20E3","\u0035\u20E3","\u0036\u20E3","\u0037\u20E3","\u0038\u20E3","\u0039\u20E3"};

	private String description;
	private String[] answers;
	private boolean usingDefaultEmoji = true;
	
	public Poll(String data) {
		description = data.replace("!poll", "").replaceAll("\\(.{1,}\\)$", "");
		Pattern multiResponse = Pattern.compile("\\(.{1,}\\)$");
	    Matcher regexMatcher = multiResponse.matcher(data);
	    if(regexMatcher.find()) {
	    	answers = regexMatcher.group(0).replaceAll("[\\(\\)]", "").split("\\|");
	    	usingDefaultEmoji = false;
	    } else {
	    	answers = new String[] { "Yes", "No" };
	    }
	}
	
	public String printPoll() {
		String poll = description;
		if(usingDefaultEmoji) {
			return poll + "\n\n :white_check_mark: for Yes\n :x: for No";
		}	
		for(int i = 0; i < Math.min(answers.length, emojis.length); i++) {
			poll += "\n "+emojis[i]+ " for "+answers[i];
		}
		return poll;
	}

	public void react(Message pollMsg) {
		if(usingDefaultEmoji) {
			pollMsg.addReaction(ReactionEmoji.unicode("\u2705")).block(); // YES
			pollMsg.addReaction(ReactionEmoji.unicode("\u274C")).block(); // NO
			return;
		}	
		for(int i = 0; i < Math.min(answers.length, unicodeEmojis.length); i++) {
			pollMsg.addReaction(ReactionEmoji.unicode(unicodeEmojis[i])).block();
		}
	}
	
	
}
