package plugins.sharedmind;

public class ExecuteMessageContent implements MessageContent {
	public String timestamp;
	public String doAction;
	public String undoAction;
	
	public ExecuteMessageContent(String timestamp, String doAction, String undoAction) {
		this.timestamp = timestamp;
		this.doAction = doAction;
		this.undoAction = undoAction;
	}
}
