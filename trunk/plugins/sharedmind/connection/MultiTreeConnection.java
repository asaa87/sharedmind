package plugins.sharedmind.connection;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import plugins.sharedmind.MapSharingController;
import plugins.sharedmind.messages.ChatMessageContent;
import plugins.sharedmind.messages.CheckpointingSuccessMessageContent;
import plugins.sharedmind.messages.ExecuteMessageContent;
import plugins.sharedmind.messages.GetMapMessageContent;
import plugins.sharedmind.messages.MapMessageContent;
import plugins.sharedmind.messages.Message;
import plugins.sharedmind.messages.RequestRetransmissionMessageContent;

import app.multicast.Msg;
import app.multicast.MsgRcvListener;
import app.multicast.MulticastComm;
import app.multicast.presence.PresenceMulticastComm;

public class MultiTreeConnection implements Connection {
	private static Logger logger = Logger
			.getLogger("plugins.MultiTreeConnection");

	private static Log logMoMo = LogFactory.getLog("MoMoAppLog");
	
	private MapSharingController mpc;
	private MulticastComm connection;
	private String topic;
	private String userName;
	private int port;

	public MultiTreeConnection(MapSharingController mpc) {
		this.mpc = mpc;
		this.topic = null;
		this.userName = null;
	}

	public void networkJoin(String user_id, int port) {
		this.userName = user_id;
		this.port = port;
//		this.connection = new MulticastComm(user_id, port + 1, port);
		this.connection = new PresenceMulticastComm(user_id, port + 1, port,
				new SharingPresenceCallback(mpc, this));
		this.connection.addMsgRcvListener(new MsgRcvListener() {
		
			public void msgReceived(Msg message) {
				logMoMo.warn("Message received: " + message.getId());
				mpc.processMessage(message.getContents());
			}
		
			public void msgLost(Map<String, Set<Integer>> arg0) {
				logger.debug(mpc.getNetworkUserId() + ": message lost");
				mpc.showCommunicationError("message lost");
			}
		});
    	mpc.initializeMessageQueue();
    	mpc.addSharingWindow();
	}

	public String getUserName() {
		return this.userName;
	}

	public void closeConnection() {
		logger.debug(userName + ": Network leave");
	}

	public void sendChat(String chat, Color color) {
		logger.debug(userName + ": Network publish: chat message");
		Message message = new Message(Message.MessageType.CHAT, userName,
				new ChatMessageContent(chat, color));
		this.connection.send(message.marshall());
	}

	public void sendGetMap(String vector_clock) {
		logger.debug(userName + ": Network publish: get map message");
		Message message = new Message(Message.MessageType.GET_MAP, userName,
				new GetMapMessageContent(vector_clock));
		this.connection.send(message.marshall());
	}

	public void sendMap(String requester, String vector_clock, String map,
			HashMap<Integer, String> checkpoint_map_list) {
		logger.debug(userName + ": Network publish: map message");
		Message message = new Message(Message.MessageType.MAP, userName,
				new MapMessageContent(requester, vector_clock, map,
						checkpoint_map_list));
		// System.out.println(message.marshall());
		this.connection.send(message.marshall());
	}

	public void sendChangeMap(String vector_clock, String map,
			HashMap<Integer, String> checkpoint_map_list) {
		logger.debug(userName + ": Network publish: change map message");
		Message message = new Message(Message.MessageType.CHANGE_MAP, userName,
				new MapMessageContent("", vector_clock, map,
						checkpoint_map_list));
		// System.out.println(message.marshall());
		this.connection.send(message.marshall());
	}

	public void sendCommand(String timestamp, String doAction, String undoAction) {
		logger.debug(userName + ": Network publish: execute message"
				+ timestamp + doAction + undoAction);
		Message message = new Message(Message.MessageType.EXECUTE, userName,
				new ExecuteMessageContent(timestamp, doAction, undoAction));
		this.connection.send(message.marshall());
	}

	@Override
	public void resendCommand(String original_sender, String timestamp,
			String doAction, String undoAction) {
		logger.debug(userName + ": Network publish: resend message"
				+ timestamp + doAction + undoAction);
		Message message = new Message(Message.MessageType.EXECUTE, original_sender,
				new ExecuteMessageContent(timestamp, doAction, undoAction));
		this.connection.send(message.marshall());
	}
	
	public void sendCheckpointingSuccess(String vector_clock,
			int version_candidate) {
		logger.debug(userName + ": Checkpointing success");
		Message message = new Message(
				Message.MessageType.CHECKPOINTING_SUCCESS, userName,
				new CheckpointingSuccessMessageContent(vector_clock,
						version_candidate));
		this.connection.send(message.marshall());
	}

	@Override
	public void sendRequestRetransmission(String from, int clock_value) {
		Message message = new Message(
				Message.MessageType.REQUEST_RETRANSMISSION, userName,
				new RequestRetransmissionMessageContent(from, clock_value));
		this.connection.send(message.marshall());
	}
	
	// message format is :" sender<sender>message"
	public void processMessage(String message_string) {
		logger.warn("message_string");
		Message message = Message.unmarshall(message_string);
		if (message.type == Message.MessageType.CHECKPOINTING_SUCCESS) {
			mpc.checkpointingSuccessReceived(message);
			return;
		}
		if (message.type == Message.MessageType.CHANGE_MAP) {
			mpc.loadMap((MapMessageContent) message.content);
			return;
		}

		if (message.sender.equals(userName))
			return;
		if (message.type == Message.MessageType.GET_MAP) {
			mpc.shareMap();
			if (mpc.hasMaxVectorClock(message.sender)) {
				mpc.addCollaborator((GetMapMessageContent) message.content);
				mpc.sendMap(message.sender);
			} else {
				mpc.addCollaborator((GetMapMessageContent) message.content);
			}
		} else if (message.type == Message.MessageType.MAP) {
			mpc.loadAndMergeMap((MapMessageContent) message.content);
		} else if (message.type == Message.MessageType.EXECUTE) {
			mpc.tryAddToMap(message);
		} else if (message.type == Message.MessageType.CHAT) {
			mpc.addChat(message.sender,
					((ChatMessageContent) message.content).chat,
					((ChatMessageContent) message.content).color);
		} else if (message.type == Message.MessageType.REQUEST_RETRANSMISSION) {
			mpc.handleRetransmissionRequest(message.sender, 
					((RequestRetransmissionMessageContent)message.content));
		}
	}

	public void setTopic(String topicID) {
		topic = topicID;
	}

	public void createTopic(String topicID) {
		logger.debug(getUserName() + ": Creating new topic: " + topic);
	}

	public void subscribeToTopic(String topicID) {
		logger.debug(userName + ": Subscribing to topic: " + topic);
		try {
			if (topicID.length() > 0) {
				String temp[] = topicID.split(":"); 
				this.connection.connect(temp[0], Integer.parseInt(temp[1]));
			} else {
				this.connection.connect("", 0);
			}
			mpc.setTopic(topicID);
		} catch (IOException e) {
			mpc.hideConnectingWindow();
		} catch (NullPointerException e) {
			mpc.hideConnectingWindow();
		}
	}

	public void unsubscribeToTopic() {
		logger.debug(userName + ": Unsubscribe topic");
		this.connection.leave();
	}

}
