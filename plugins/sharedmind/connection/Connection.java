package plugins.sharedmind.connection;

import java.awt.Color;
import java.util.HashMap;

public interface Connection {
	public String getUserName();

	public void networkJoin(String user_id, int port);

	public void closeConnection();

	public void sendChat(String chat, Color color);

	public void sendGetMap(String vector_clock);

	public void sendMap(String requester, String vector_clock, String map,
			HashMap<Integer, String> checkpoint_map_list);

	public void sendChangeMap(String vector_clock, String map,
			HashMap<Integer, String> checkpoint_map_list);

	public void sendCommand(String timestamp, String doAction, String undoAction);
	
	public void resendCommand(String original_sender, String timestamp, String doAction, String undoAction);

	public void sendCheckpointingSuccess(String vector_clock,
			int version_candidate);

	// message format is :" sender<sender>message"
	public void processMessage(String message_string);

	public void setTopic(String topicID);

	public void createTopic(String topicID);

	public void subscribeToTopic(String topicID);

	public void unsubscribeToTopic();

	public void sendRequestRetransmission(String from, int clock_value);
}
