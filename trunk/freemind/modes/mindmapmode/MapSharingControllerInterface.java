package freemind.modes.mindmapmode;

import java.util.Vector;

import freemind.modes.MindMapNode;
import freemind.modes.ModeController;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

public interface MapSharingControllerInterface {

	/**
	 * Add new Node to map
	 * 
	 * @param pair
	 * @param - still no idea ;)
	 */

	public abstract void addnewAction(ActionPair pair);

	public abstract void sendMap(String requester);

	public abstract void sendChangeMap(MindMapController final_merged_map);

	public abstract void unregister();

	public abstract void stopCollaboration();

	public abstract void setCurrentEditedNode(MindMapNode selected);

	public abstract void setMindMapController(ModeController newModeController);

	/**
	 * Initialise message_queue and checkpoint
	 */
	public abstract void initializeMessageQueue();

	public abstract void networkJoin(String user_id, int port);

	public abstract void processMessage(String message);

	public abstract void setTopic(String topicID);

	public abstract void createTopic(String topic);

	public abstract void addSharingWindow();

	public abstract void subscribeToTopic(String topic);

	public abstract void unsubscribeToTopic();

	public abstract void sendChat(String chat);

	public abstract void addChat(String sender, String message);

	public abstract void exitSharing();

	public abstract void requestMap();

	public abstract boolean hasMaxVectorClock(String exception);

	public abstract void showCommunicationError(String error_message);

	public abstract MindMapController getController();

	public abstract String getNetworkUserId();

	public abstract int getNetworkPort();

	public abstract void showGetMapWindow();

	public abstract void showConnectingWindow();

	public abstract void hideConnectingWindow();

	/**
	 * Mark that this peer already has the map and initialise
	 * checkpoint_in_progress and last_successful_checkpoint
	 */
	public abstract void setHasMap();

	public abstract void sendCheckpointingCompleteMessage(
			String vector_clock_string, int version_number_candidate);

	public abstract void startCheckpointing();

	public abstract void stopCheckpointing();

	public abstract void onMergeFinished();

	public abstract void shareMap();

	public abstract void stopSharingMap();

	public abstract void updateOnlineUserList(Vector<String> user_list);

	public abstract void onVersionChange(int version);

	public abstract void setPropagateFoldAction(boolean b);

}