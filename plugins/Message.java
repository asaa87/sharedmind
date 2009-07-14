package plugins;

import com.thoughtworks.xstream.XStream;

public class Message {
	public static enum MessageType {
		GET_MAP, MAP, EXECUTE, CHECKPOINTING_SUCCESS, CHANGE_MAP, CHAT;
	};
	
	public MessageType type;
    public String sender;
    public MessageContent content;
    
    public Message(MessageType type, String sender, MessageContent content) {
    	this.type = type;
    	this.sender = sender;
    	this.content = content;
    }
    
	public String marshall() {
		XStream xstream = new XStream();
		return xstream.toXML(this);
	}
	
	public static Message unmarshall(String xml) {
		XStream xstream = new XStream();
		return (Message)xstream.fromXML(xml);
	}
}
