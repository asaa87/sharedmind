package plugins;

import java.util.HashMap;

import org.apache.log4j.Logger;

import pl.edu.pjwstk.mteam.core.AbstractPubSubManager;
import pl.edu.pjwstk.mteam.core.NoSuchSubscriberException;
import pl.edu.pjwstk.mteam.core.NodeCallback;
import pl.edu.pjwstk.mteam.pubsub.presence.PresenceCallback;
import pl.edu.pjwstk.mteam.pubsub.presence.PresenceP2PNode;
import pl.edu.pjwstk.mteam.pubsub.presence.PresencePubSubManager;
import pl.edu.pjwstk.p2p.P2PNode;

public class P2PPConnection implements Connection {
    private static Logger logger = Logger.getLogger("plugins.P2PPConnection");
    
	private P2PNode node;
	private MapSharingController mpc;
	private String topic;
	private String userName;
    
	public P2PPConnection(MapSharingController mpc) {
		this.mpc = mpc;
		NodeCallback sharing_actions = new SharingActions(mpc);
		PresenceCallback presence_callback = new SharingPresenceCallback(mpc, this);
    	this.node = new PresenceP2PNode(sharing_actions, presence_callback);
    	this.topic = null;
    }

	public String getUserName() {
		return this.userName;
	}
	
	public void networkJoin(String user_id, int port) {
		this.userName = user_id;
		logger.debug(user_id + ": Set user name");
    	node.setUserName(user_id);
    	logger.debug(user_id + ": Set tcp and udp ports");
    	node.setTcpPort(port);
    	node.setUdpPort(port);
		AbstractPubSubManager pubSubManager = new PresencePubSubManager(port + 1, node);
		logger.debug(user_id + ": Network join, port:" + port);
    	node.networkJoin();
	}

	public void closeConnection() {
		logger.debug(userName + ": Network leave");
	    node.networkLeave();	
	}
	
	public void sendChat(String chat) {
		logger.debug(userName + ": Network publish: chat message");
		Message message = new Message(Message.MessageType.CHAT, userName,
				                      new ChatMessageContent(chat));
		node.getPubSubManager().networkPublish(topic, message.marshall());
	}
	
	public void sendGetMap(String vector_clock) {
		logger.debug(userName + ": Network publish: get map message");
		Message message = new Message(Message.MessageType.GET_MAP, userName,
                new GetMapMessageContent(vector_clock));
		node.getPubSubManager().networkPublish(topic, message.marshall());
	}
	
	public void sendMap(String requester, String vector_clock, String map, 
			HashMap<Integer, String> checkpoint_map_list) {
		logger.debug(userName + ": Network publish: map message");
		Message message = new Message(Message.MessageType.MAP, userName,
                new MapMessageContent(requester, vector_clock, map, checkpoint_map_list));
		//System.out.println(message.marshall());
		node.getPubSubManager().networkPublish(topic, message.marshall());	
	}
	
	public void sendChangeMap(String vector_clock, String map, 
			HashMap<Integer, String> checkpoint_map_list) {
		logger.debug(userName + ": Network publish: change map message");
		Message message = new Message(Message.MessageType.CHANGE_MAP, userName,
                new MapMessageContent("", vector_clock, map, checkpoint_map_list));
		//System.out.println(message.marshall());
		node.getPubSubManager().networkPublish(topic, message.marshall());	
	}

	public void sendCommand(String timestamp, String doAction, String undoAction) {
		logger.debug(userName + ": Network publish: execute message" + timestamp + doAction + undoAction);
		Message message = new Message(Message.MessageType.EXECUTE, userName,
                new ExecuteMessageContent(timestamp, doAction, undoAction));
		node.getPubSubManager().networkPublish(topic, message.marshall());	
	}
	
	public void sendCheckpointingSuccess(String vector_clock, int version_candidate) {
		logger.debug(userName + ": Checkpointing success");
		Message message = new Message(Message.MessageType.CHECKPOINTING_SUCCESS, userName,
                new CheckpointingSuccessMessageContent(vector_clock, version_candidate));
		node.getPubSubManager().networkPublish(topic, message.marshall());	
	}

	// message format is :" sender<sender>message"
	public void processMessage(String message_string) {
		Message message = Message.unmarshall(message_string);
		if (message.type == Message.MessageType.CHECKPOINTING_SUCCESS){
       		mpc.checkpointingSuccessReceived(message);
       		return;
       	}
		if (message.type == Message.MessageType.CHANGE_MAP) {
       		mpc.loadMap((MapMessageContent)message.content);
       		return;
       	} 
		
		if (message.sender.equals(userName))
			return;
       	if (message.type == Message.MessageType.GET_MAP) {
       		if (mpc.hasMaxVectorClock()) {
       			mpc.addCollaborator((GetMapMessageContent)message.content);
       		    mpc.sendMap(message.sender);
       		} else {
       			mpc.addCollaborator((GetMapMessageContent)message.content);
       		}
       	} else if (message.type == Message.MessageType.MAP) {
       		mpc.loadAndMergeMap((MapMessageContent)message.content);
       	} else if (message.type == Message.MessageType.EXECUTE) {
    		mpc.tryAddToMap(message);
       	} else if (message.type == Message.MessageType.CHAT) {
		    mpc.addChat(message.sender, ((ChatMessageContent)message.content).chat);
       	}
    }

	public void setTopic(String topicID) {
		topic = topicID;
	}

	public void createTopic(String topicID) {
		logger.debug(getUserName() + ": Creating new topic: " + topic);
		node.getPubSubManager().createTopic(topicID);
	}

	public void subscribeToTopic(String topicID) {
		logger.debug(userName + ": Subscribing to topic: " + topic);
		node.getPubSubManager().networkSubscribe(topicID);
	}
	
	public void unsubscribeToTopic() {
		try {
			if (topic != null)
				logger.debug(userName + ": Unsubscribe topic");
			    node.getPubSubManager().networkUnsubscribe(topic);
		} catch (NoSuchSubscriberException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
