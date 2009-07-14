package plugins;

public class CheckpointingSuccessMessageContent implements MessageContent {
	public String vector_clock;
	public int version_candidate;
	
	public CheckpointingSuccessMessageContent(String vector_clock, int version_candidate) {
		this.vector_clock = vector_clock;
		this.version_candidate = version_candidate;
	}
}
