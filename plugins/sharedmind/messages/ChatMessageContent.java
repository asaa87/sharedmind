package plugins.sharedmind.messages;

public class ChatMessageContent implements MessageContent {
	public String chat;
	
	public ChatMessageContent(String chat) {
		this.chat = chat;
	}
}
