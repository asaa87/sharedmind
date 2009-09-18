package plugins.sharedmind.messages;

import java.util.Vector;



public class RequestRetransmissionMessageContent implements MessageContent {
    public String original_sender;
    public int missing_message;
    
    public RequestRetransmissionMessageContent(
    		String original_sender, int missing_message) {
    	this.original_sender = original_sender;
    	this.missing_message = missing_message;
    }
}