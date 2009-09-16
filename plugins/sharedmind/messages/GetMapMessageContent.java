package plugins.sharedmind.messages;

public class GetMapMessageContent implements MessageContent {
	public String vector_clock;
	
	public GetMapMessageContent(String vector_clock) {
		this.vector_clock = vector_clock;
	}
}
