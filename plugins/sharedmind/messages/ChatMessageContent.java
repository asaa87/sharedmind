package plugins.sharedmind.messages;

import java.awt.Color;

public class ChatMessageContent implements MessageContent {
	public String chat;
	public Color color;
	
	public ChatMessageContent(String chat, Color color) {
		this.chat = chat;
		this.color = color;
	}
}
