package plugins.sharedmind;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import plugins.sharedmind.checkpoint.Checkpoint;
import plugins.sharedmind.checkpoint.CheckpointList;
import plugins.sharedmind.checkpoint.ReadOnlyCheckpointList;
import plugins.sharedmind.connection.Connection;
import plugins.sharedmind.connection.MultiTreeConnection;
import plugins.sharedmind.gmomo.ContactList;
import plugins.sharedmind.gmomo.InvitationListener;
import plugins.sharedmind.gmomo.PresenceListener;
import plugins.sharedmind.merging.MapsDiff;
import plugins.sharedmind.merging.MergedMap;
import plugins.sharedmind.merging.MapsDiff.ChangeList.Change;
import plugins.sharedmind.messages.CheckpointingSuccessMessageContent;
import plugins.sharedmind.messages.ExecuteMessageContent;
import plugins.sharedmind.messages.GetMapMessageContent;
import plugins.sharedmind.messages.MapMessageContent;
import plugins.sharedmind.messages.Message;
import plugins.sharedmind.messages.RequestRetransmissionMessageContent;
import plugins.sharedmind.synchronouscollaboration.MessageQueue;
import plugins.sharedmind.synchronouscollaboration.SharedAction;
import plugins.sharedmind.synchronouscollaboration.SynchronousEditingHistory;
import plugins.sharedmind.synchronouscollaboration.VectorClock;
import plugins.sharedmind.view.ConflictWindow;
import plugins.sharedmind.view.ConnectingWindow;
import plugins.sharedmind.view.GetMapWindow;
import plugins.sharedmind.view.MergingWindow;
import plugins.sharedmind.view.P2PPSharingLoginWindow;
import plugins.sharedmind.view.RedoConflictingActionsWindow;
import plugins.sharedmind.view.SharingWindow;
import plugins.sharedmind.view.TransportErrorWindow;
import plugins.sharedmind.view.gmomo.ContactListWindow;
import plugins.sharedmind.view.gmomo.InvitationWindow;
import momo.app.multicast.presence.Presence;
import freemind.common.XmlBindingTools;
import freemind.controller.Controller;
import freemind.controller.MapModuleManager;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.main.XMLParseException;
import freemind.modes.MindMapNode;
import freemind.modes.ModeController;
import freemind.modes.mindmapmode.MapSharingControllerInterface;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.MindMapMapModel;
import freemind.modes.mindmapmode.actions.xml.ActionPair;
import freemind.modes.mindmapmode.actions.xml.SharingActionFactory;
import freemind.view.MapModule;
import gmomo.GMoMoConfig;
import gmomo.GMoMoConnection;
import gmomo.PacketListenerImpl;
import gmomo.packet.Invitation;
import gmomo.test.gui.GUIInvitationListener;
import gmomo.test.gui.GUIMessageListener;
import gmomo.test.gui.GUIPresenceListener;

public class MapSharingController implements MapSharingControllerInterface {
	private static String XMPP_RESOURCE_STRING = "sharedmind";
	private static String GMOMO_CONFIG_FILE = "plugins/sharedmind/lib/TreeComm/config/config.properties";
	private Logger log = Logger.getLogger(MapSharingController.class);
	
	private P2PPSharingLoginWindow login_window;
	private Connection connection;

	private SharingWindow sharing_window;
	private ConnectingWindow connecting_window;
	private Color chat_color;
	private MindMapController mm_controller;
	private MindMapNode currently_edited_node;
	private MessageQueue message_queue;
	private SynchronousEditingHistory synchronous_editing_history;
	// for checkpointing
	private Checkpoint last_successful_checkpoint;
	private Checkpoint checkpoint_in_progress;
	private CheckpointList checkpoint_list;
	private boolean has_map;
	private boolean map_shared;
	XmlBindingTools test;
	// for gmomo
	private ContactList gmomo_contact_list;
	private GMoMoConnection gmomo_connection;
	private ContactListWindow contact_list_window;

	/**
	 * userId used for joining the network
	 */
	private String networkUserId;
	/**
	 * port used for joining the network
	 */
	private int networkPort;
	private MergedMap merged_map;
	private MindMapController last_common_map;

	public MapSharingController(MindMapController mmcontroller) {
		this.mm_controller = mmcontroller;
		mm_controller.getController().registerMapSharingController(this);
		test = XmlBindingTools.getInstance();
		this.currently_edited_node = null;
		this.login_window = new P2PPSharingLoginWindow(this);
		this.connection = null;
		try {
			this.gmomo_connection = new GMoMoConnection(GMoMoConfig.load(GMOMO_CONFIG_FILE));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.has_map = false;
		this.synchronous_editing_history = new SynchronousEditingHistory(this);
		this.checkpoint_list = new CheckpointList(this);
		this.chat_color = Color.black;
		login_window.setVisible(true);
		Presence.setPresenceInterval(60000);
		merged_map = null;
		map_shared = false;
	}

	/**
	 * UserID shouldn't be a String - something else. message - received message
	 * which we should show on the ChatBox
	 */

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#addnewAction(freemind.modes.mindmapmode.actions.xml.ActionPair)
	 */

	public void addnewAction(ActionPair pair) {
		message_queue.getVectorClock().incrementClock(connection.getUserName());
		this.synchronous_editing_history.addToHistory(
				new SharedAction (connection.getUserName(), message_queue.getVectorClock().clone(), pair));
		// only send if no checkpointing is in progress
		if (checkpoint_in_progress == null) {
			sendLocalAction(message_queue.getVectorClock().clone(), pair);
		} else {
			checkpoint_in_progress.addLocalAction(message_queue
					.getVectorClock().clone(), pair);
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#sendLocalAction(plugins.sharedmind.VectorClock, freemind.modes.mindmapmode.actions.xml.ActionPair)
	 */
	public void sendLocalAction(VectorClock timestamp, ActionPair pair) {
		String doActionString = test.marshall(pair.getDoAction());
		String doUndoAction = test.marshall(pair.getUndoAction());
		try {
			connection.sendCommand(timestamp.toString(), doActionString,
					doUndoAction);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#tryAddToMap(plugins.sharedmind.Message)
	 */
	public synchronized void tryAddToMap(Message message) {
		if (!this.map_shared)
			return;
		ExecuteMessageContent content = (ExecuteMessageContent) message.content;
		log.debug("Try add to map: " + content.timestamp);
		VectorClock timestamp = new VectorClock(content.timestamp);
		XmlAction doAction = test.unMarshall(content.doAction);
		XmlAction undoAction = test.unMarshall(content.undoAction);
		log.debug("doAction: " + doAction);
		log.debug("undoAction: " + undoAction);
		ActionPair action_pair = new ActionPair(doAction, undoAction);
		SharedAction queued_message = new SharedAction(
				message.sender, timestamp, action_pair);
		if (doAction instanceof NodeAction && currently_edited_node != null) {
			String node = ((NodeAction) (doAction)).getNode();
			System.out.println(node);
			if (node.equals(currently_edited_node.getObjectId(mm_controller))) {
				ConflictWindow.ShowConflictWindow(mm_controller.getFrame().getJFrame());
			}
		}
		Vector<SharedAction> messages = message_queue
				.enqueueAndReturnAllThatCanBeExecuted(queued_message);
		// Apply remote action to checkpointed data if checkpointing is in
		// progress
		if (checkpoint_in_progress != null) {
			checkpoint_in_progress.addRemoteActions(messages);
		}
		for (SharedAction current_message : messages) {
			addToMap(current_message);
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#addToMap(plugins.sharedmind.MessageQueue.Message)
	 */
	public synchronized void addToMap(SharedAction message) {
		log.debug("Add to map: " + message.getTimestamp().toString());
		Vector<SharedAction> conflicting = this.synchronous_editing_history.getConflictingChanges(message);
		if (conflicting.isEmpty()) {
			
			Vector<SharedAction> following_actions = 
					this.synchronous_editing_history.getFollowingActions(message);
			
			log.warn(following_actions.toString());
			// undo actions
			for (int i = following_actions.size() - 1; i >= 0; --i) {
				SharedAction cancel_action = following_actions.get(i);
				ActionPair undoAction = 
					new ActionPair(cancel_action.getActionPair().getUndoAction(), 
							cancel_action.getActionPair().getDoAction());
				((SharingActionFactory) mm_controller.getActionFactory())
					.remoteExecuteAction(undoAction);
			}
			// do new action
			((SharingActionFactory) mm_controller.getActionFactory())
				.remoteExecuteAction(message.getActionPair());
			// redo actions
			for (SharedAction redo_action : following_actions) {
				((SharingActionFactory) mm_controller.getActionFactory())
					.remoteExecuteAction(redo_action.getActionPair());
			}
		} else {
			ConflictWindow.ShowConflictWindow(mm_controller.getFrame().getJFrame());
			message.setUndoed(true);
			for (int i = conflicting.size() - 1; i >= 0; --i) {
				SharedAction cancel_action = conflicting.get(i);
				log.warn("cancel" + cancel_action.getActionPair().getDoAction().toString());
				if (!cancel_action.isUndoed()) {
					cancel_action.setUndoed(true);
					ActionPair undoAction = 
						new ActionPair(cancel_action.getActionPair().getUndoAction(), 
								cancel_action.getActionPair().getDoAction());
					((SharingActionFactory) mm_controller.getActionFactory())
						.remoteExecuteAction(undoAction);
				}
			}
		}
		message_queue.getVectorClock().adjustWithTimestamp(
				message.getTimestamp());
		this.synchronous_editing_history.addToHistory(message);
		if (message.isUndoed()) {
			RedoConflictingActionsWindow.showRedoConflictingActionsWindow(mm_controller.getController().getJFrame(), this);
		}
		mm_controller.repaintMap();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#sendMap(java.lang.String)
	 */
	public void sendMap(String requester) {
		if (last_successful_checkpoint != null) {
			last_successful_checkpoint.saveCheckpointToFile();
		}
		mm_controller.save();
		StringWriter body = new StringWriter();
		try {
			mm_controller.getMap().getXml(body);
		} catch (Exception err) {
			err.printStackTrace();
		}
		try {
			HashMap<Integer, String> checkpoint_file_list = this.checkpoint_list
					.getCheckpointFileList();
			HashMap<Integer, String> checkpoint_map_list = new HashMap<Integer, String>();
			for (Map.Entry<Integer, String> entry : checkpoint_file_list
					.entrySet()) {
				File file = new File(entry.getValue());
				FileReader reader = new FileReader(file);
				StringBuffer buffer = new StringBuffer();
				int temp;
				while ((temp = reader.read()) != -1) {
					buffer.append((char) temp);
				}
				reader.close();
				checkpoint_map_list.put(entry.getKey(), buffer.toString());
			}
			connection.sendMap(requester, message_queue.getVectorClock()
					.toString(), body.toString(), checkpoint_map_list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#sendChangeMap(freemind.modes.mindmapmode.MindMapController)
	 */
	public void sendChangeMap(MindMapController final_merged_map) {
		StringWriter body = new StringWriter();
		try {
			final_merged_map.getMap().getXml(body);
			log.warn(body.toString());
		} catch (Exception err) {
			err.printStackTrace();
		}
		try {
			HashMap<Integer, String> checkpoint_file_list = new ReadOnlyCheckpointList(
					final_merged_map).getCheckpointFileList();
			HashMap<Integer, String> checkpoint_map_list = new HashMap<Integer, String>();
			for (Map.Entry<Integer, String> entry : checkpoint_file_list
					.entrySet()) {
				File file = new File(entry.getValue());
				FileReader reader = new FileReader(file);
				StringBuffer buffer = new StringBuffer();
				int temp;
				while ((temp = reader.read()) != -1) {
					buffer.append((char) temp);
				}
				reader.close();
				checkpoint_map_list.put(entry.getKey(), buffer.toString());
			}
			connection.sendChangeMap(message_queue.getVectorClock().toString(),
					body.toString(), checkpoint_map_list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#loadAndMergeMap(plugins.sharedmind.MapMessageContent)
	 */
	public void loadAndMergeMap(MapMessageContent content) {
		if (this.connection.getUserName().equals(content.requester)) {
			if (has_map) {
				File local_map = mm_controller.getMap().getFile();
				mm_controller.save();
				loadMap(content);
				try {
					mergeMap(local_map.toURI().toURL(), mm_controller.getModel()
							.getFile().toURI().toURL());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			} else {
				loadMap(content);
			}
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#loadMap(plugins.sharedmind.MapMessageContent)
	 */
	public void loadMap(MapMessageContent content) {
		log.debug("loading map");
		this.stopSharingMap();
		Controller controller = mm_controller.getController();
		MapModuleManager map_module_manager = controller.getMapModuleManager();
		List<MapModule> map_modules = map_module_manager.getMapModuleVector();
		String display_name = "";
		for (MapModule map_module : map_modules) {
			if (map_module.getModeController() == this.mm_controller)
				display_name = map_module.getDisplayName();
		}
		log.debug("display name: " + display_name);
		map_module_manager.tryToChangeToMapModule(display_name);
		mm_controller.load(content.map);
		mm_controller = (MindMapController) controller.getMapModule()
				.getModeController();
		VectorClock vector_clock = new VectorClock(content.vector_clock);
		Vector<String> participants = this.message_queue.getCurrentParticipant();
		this.message_queue = new MessageQueue(this, connection.getUserName(),
				vector_clock);
		this.message_queue.setCurrentParticipant(participants);
		this.checkpoint_list = new CheckpointList(this);
		this.checkpoint_in_progress = null;
		this.last_successful_checkpoint = null;
		try {
			for (Map.Entry<Integer, String> entry : content.checkpoint_list
					.entrySet()) {
				File file = File.createTempFile("CHECKPOINT_"
						+ mm_controller.getMap().getFile().getName(), "mm",
						mm_controller.getMap().getFile().getParentFile());
				FileWriter writer = new FileWriter(file);
				writer.write(entry.getValue());
				writer.close();
				this.checkpoint_list.changeCheckpointFileName(entry.getKey(),
						file.getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		mm_controller.getModel().setSaved(false);
		mm_controller.save();
		this.shareMap();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#unregister()
	 */
	public void unregister() {
		mm_controller.getController().deregisterMapSharingController(this);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#stopCollaboration()
	 */
	public void stopCollaboration() {
		this.unregister();
		this.unsubscribeToTopic();
		this.connection.closeConnection();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setCurrentEditedNode(freemind.modes.MindMapNode)
	 */
	public synchronized void setCurrentEditedNode(MindMapNode selected) {
		if (selected != null
				&& message_queue.editConflicting(selected
						.getObjectId(mm_controller))) {
			ConflictWindow.ShowConflictWindow(mm_controller.getFrame().getJFrame());
		}
		this.currently_edited_node = selected;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setMindMapController(freemind.modes.ModeController)
	 */
	public void setMindMapController(ModeController newModeController) {
		if (newModeController instanceof MindMapController)
			this.mm_controller = (MindMapController) newModeController;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#initializeMessageQueue()
	 */
	public void initializeMessageQueue() {
		VectorClock vector_clock = new VectorClock();
		this.message_queue = new MessageQueue(this, connection.getUserName(),
				vector_clock);
		vector_clock.addCollaborator(connection.getUserName(),
				(int) (Math.random() * Integer.MAX_VALUE));
		this.checkpoint_in_progress = null;
		this.last_successful_checkpoint = null;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#networkJoin(java.lang.String, int)
	 */
	public void networkJoin(String user_id, int port) {
		// connection = new P2PPConnection(this);
		this.networkUserId = user_id;
		this.networkPort = port;
		connection = new MultiTreeConnection(this);
		connection.networkJoin(user_id, port);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#processMessage(java.lang.String)
	 */
	public void processMessage(String message) {
		connection.processMessage(message);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setTopic(java.lang.String)
	 */
	public void setTopic(String topicID) {
		connection.setTopic(topicID);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#createTopic(java.lang.String)
	 */
	public void createTopic(String topic) {
		connection.createTopic(topic);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#addSharingWindow()
	 */
	public void addSharingWindow() {
		login_window.setVisible(false);
		login_window.dispose();
		this.sharing_window = new SharingWindow(this);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#subscribeToTopic(java.lang.String)
	 */
	public void subscribeToTopic(String topic) {
		System.out.println("create connecting window");
		showConnectingWindow();
		connection.subscribeToTopic(topic);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#unsubscribeToTopic()
	 */
	public void unsubscribeToTopic() {
		connection.unsubscribeToTopic();
		this.sharing_window.addChat("--- Disconnected ---", Color.black);
		this.stopSharingMap();
		if (last_successful_checkpoint != null) {
			last_successful_checkpoint.saveCheckpointToFile();
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#sendChat(java.lang.String)
	 */
	public void sendChat(String chat) {
		connection.sendChat(chat, this.chat_color);
		addChat("me", chat, this.chat_color);
		if (chat.startsWith("__test")) {
			test(chat);
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#addChat(java.lang.String, java.lang.String)
	 */
	public void addChat(String sender, String message, Color color) {
		sharing_window.addChat(sender + ": " + message, color);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#exitSharing()
	 */
	public void exitSharing() {
		connection.closeConnection();
		unregister();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#requestMap()
	 */
	public void requestMap() {
		connection.sendGetMap(this.message_queue.getVectorClock().toString());
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#addCollaborator(plugins.sharedmind.GetMapMessageContent)
	 */
	public void addCollaborator(GetMapMessageContent content) {
		this.message_queue.addCollaborators(content.vector_clock);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#hasMaxVectorClock(java.lang.String)
	 */
	public boolean hasMaxVectorClock(String exception) {
		return this.message_queue.hasMaxVectorClock(exception);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#showCommunicationError(java.lang.String)
	 */
	public void showCommunicationError(String error_message) {
		if (sharing_window == null)
			return;
		TransportErrorWindow error_window = new TransportErrorWindow(
				sharing_window, error_message);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getController()
	 */
	public MindMapController getController() {
		return mm_controller;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getNetworkUserId()
	 */
	public String getNetworkUserId() {
		return networkUserId;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getNetworkPort()
	 */
	public int getNetworkPort() {
		return networkPort;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getMessageQueue()
	 */
	public MessageQueue getMessageQueue() {
		return message_queue;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#showGetMapWindow()
	 */
	public void showGetMapWindow() {
		this.sharing_window.addChat("--- Connected ---", Color.black);
		new GetMapWindow(sharing_window, this);
	}
	
	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#showConnectingWindow()
	 */
	public void showConnectingWindow() {
		this.connecting_window = new ConnectingWindow(sharing_window, this);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#hideConnectingWindow()
	 */
	public void hideConnectingWindow() {
		this.connecting_window.hide();
	}
	
	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setHasMap()
	 */
	public void setHasMap() {
		this.has_map = true;
		this.checkpoint_in_progress = null;
		this.last_successful_checkpoint = null;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getCheckpointInProgress()
	 */
	public Checkpoint getCheckpointInProgress() {
		return checkpoint_in_progress;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#sendCheckpointingCompleteMessage(java.lang.String, int)
	 */
	public void sendCheckpointingCompleteMessage(String vector_clock_string,
			int version_number_candidate) {
		connection.sendCheckpointingSuccess(vector_clock_string,
				version_number_candidate);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#startCheckpointing()
	 */
	public void startCheckpointing() {
		checkpoint_in_progress = new Checkpoint(this,
				last_successful_checkpoint);
		sharing_window.startCheckpointing();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#stopCheckpointing()
	 */
	public void stopCheckpointing() {
		this.checkpoint_in_progress = null;
		sharing_window.stopCheckpointing();
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setLastSuccessfulCheckpoint(plugins.sharedmind.Checkpoint)
	 */
	public void setLastSuccessfulCheckpoint(
			Checkpoint last_successful_checkpoint) {
		this.last_successful_checkpoint = last_successful_checkpoint;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#checkpointingSuccessReceived(plugins.sharedmind.Message)
	 */
	public void checkpointingSuccessReceived(Message message) {
		if (checkpoint_in_progress != null) {
			CheckpointingSuccessMessageContent content = (CheckpointingSuccessMessageContent) message.content;
			checkpoint_in_progress
					.onReceiveCheckpointSuccessMessage(message.sender,
							content.vector_clock, content.version_candidate);
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getCheckpointList()
	 */
	public CheckpointList getCheckpointList() {
		return checkpoint_list;
	}

	private void test(String chat) {
		String[] temp = chat.split(" ");
		MindMapController base_map = (MindMapController) getController()
				.getMode().createModeController();
		new MindMapMapModel(getController().getModel().getFrame(), base_map);
		MindMapController v1_map = (MindMapController) getController()
				.getMode().createModeController();
		new MindMapMapModel(getController().getModel().getFrame(), v1_map);
		MindMapController v2_map = (MindMapController) getController()
				.getMode().createModeController();
		new MindMapMapModel(getController().getModel().getFrame(), v2_map);

		try {
			File file = new File(temp[1]);
			base_map = (MindMapController) base_map.load(file.toURI().toURL());
			file = new File(temp[2]);
			v1_map = (MindMapController) v1_map.load(file.toURI().toURL());
			file = new File(temp[3]);
			v2_map = (MindMapController) v2_map.load(file.toURI().toURL());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MergedMap merged_map = new MergedMap(this, base_map, v1_map, v2_map);
		MindMapController map = merged_map.getMergedMap();
		MindMapController new_map = (MindMapController) getController()
				.getMode().createModeController();
		new MindMapMapModel(getController().getModel().getFrame(), new_map);
		try {
			File file = File.createTempFile("aaaaa", "mm");
			FileWriter writer = new FileWriter(file);
			map.getModel().getXml(writer);
			writer.close();
			new_map.load(file.toURI().toURL());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLParseException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		MapsDiff diff = merged_map.getMapDiff1();
		Vector<Change> list = diff.getChangesList().getList();
		for (Change change : list) {
			sharing_window.addChat(change.toString(), Color.black);
		}
		sharing_window.addChat("-------------", Color.black);
		diff = merged_map.getMapDiff2();
		list = diff.getChangesList().getList();
		for (Change change : list) {
			sharing_window.addChat(change.toString(), Color.black);
		}
	}

	private void mergeMap(URL local_map, URL current_map) {
		MindMapController local_map_controller = (MindMapController) mm_controller
				.getMode().createModeController();
		new MindMapMapModel(mm_controller.getFrame(), local_map_controller);
		MindMapController current_map_controller = (MindMapController) mm_controller
				.getMode().createModeController();
		new MindMapMapModel(mm_controller.getFrame(), current_map_controller);
//		MindMapController common_map_controller = (MindMapController) mmController
//				.getMode().createModeController();
//		new MindMapMapModel(mmController.getFrame(), common_map_controller);
		log.debug(local_map.toString());
		log.debug(current_map.toString());
		try {
			local_map_controller.getModel().load(local_map);
			current_map_controller.getModel().load(current_map);
			
			StringWriter fileout = new StringWriter();
			current_map_controller.getModel().getXml(fileout);
			log.debug(fileout.toString());
			mergeMap(local_map_controller, current_map_controller);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private void mergeMap(MindMapController local_map_controller,
			MindMapController current_map_controller) {
		MindMapController common_map_controller = (MindMapController) mm_controller
				.getMode().createModeController();
		new MindMapMapModel(mm_controller.getFrame(), common_map_controller);
		try {
			String common_map = this.getCommonMapFile(local_map_controller,
					current_map_controller);
			sharing_window.addChat(common_map, Color.black);
			File common_map_file = new File(common_map);
			common_map_controller.getModel().load(
					common_map_file.toURI().toURL());
			mergeMap(common_map_controller, local_map_controller, current_map_controller);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private void mergeMap(MindMapController common_map_controller, 
			MindMapController local_map_controller, MindMapController current_map_controller) {
		sharing_window.addChat("------------------merging map---------------------", Color.black);
		try {
			last_common_map = current_map_controller;
			merged_map = new MergedMap(this, common_map_controller,
					local_map_controller, current_map_controller);
			System.out.println(merged_map.getConflictList().getList().toString());
			if (merged_map.getConflictList().getList().isEmpty()) {
				sharing_window.addChat("-------------no conflict--------------", Color.black);
				MindMapController final_map = merged_map.finalizedMergedMap();
				this.sendChangeMap(final_map);
			} else {
				sharing_window.addChat("-------------conflict--------------", Color.black);
				merged_map.showMergingMap();
				MergingWindow merging_window = 
					new MergingWindow(mm_controller.getController().getJFrame(), this);
			}
		} catch (XMLParseException e) {
			e.printStackTrace();
		}
	}
	
	private String getCommonMapFile(MindMapController mmController1,
			MindMapController mmController2) {
		ReadOnlyCheckpointList checkpoint_list1 = new ReadOnlyCheckpointList(
				mmController1);
		ReadOnlyCheckpointList checkpoint_list2 = new ReadOnlyCheckpointList(
				mmController2);

		int version1 = checkpoint_list1.getLatestCheckpointVersion();
		while (version1 != ReadOnlyCheckpointList.NO_CHECKPOINT_VERSION) {
			int version2 = checkpoint_list2.getLatestCheckpointVersion();
			while (version2 != ReadOnlyCheckpointList.NO_CHECKPOINT_VERSION) {
				if (version1 == version2)
					return checkpoint_list1.getCheckpointedFileName(version1);
				version2 = checkpoint_list2
						.getPreviousCheckpointVersion(version2);
			}
			version1 = checkpoint_list1.getPreviousCheckpointVersion(version1);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#onMergeFinished()
	 */
	public void onMergeFinished() {
		MindMapController finalized_merged_map = merged_map
				.finalizedMergedMap();
		mergeMap(last_common_map, finalized_merged_map, mm_controller);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getConnection()
	 */
	public Connection getConnection() {
		return this.connection;
	}
	
	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#shareMap()
	 */
	public void shareMap() {
		if (!this.map_shared) {
			this.map_shared = true;
			mm_controller.shareMap();
			sharing_window.setVersion(Integer.parseInt(
					(String) mm_controller.getModel().getRegistry().getAttributes()
					.getElement("VERSION").getValues().firstElement()));
		}
		sharing_window.propagateFoldActionEnabled(true);
	}
	
	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#stopSharingMap()
	 */
	public void stopSharingMap() {
		if (this.map_shared) {
			this.map_shared = false;
			mm_controller.stopSharingMap();
			sharing_window.propagateFoldActionEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#updateOnlineUserList(java.util.Vector)
	 */
	public void updateOnlineUserList(Vector<String> user_list) {
		sharing_window.setOnlineUserList(user_list);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#onVersionChange(int)
	 */
	public void onVersionChange(int version) {
		sharing_window.setVersion(version);
	}

	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#setPropagateFoldAction(boolean)
	 */
	public void setPropagateFoldAction(boolean b) {
		((SharingActionFactory) mm_controller.getActionFactory()).setPropagate_folding_action(b);
	}
	
	/* (non-Javadoc)
	 * @see plugins.sharedmind.MapSharingControllerInterface#getSynchronousEditingHistory()
	 */
	public SynchronousEditingHistory getSynchronousEditingHistory() {
		return this.synchronous_editing_history;
	}

	public boolean showConflict(boolean next) {
		return this.merged_map.showConflict(next);
	}
	
	public boolean markAsResolved() {
		return this.merged_map.markAsResolved();
	}
	
	public int getNumberOfResolvedConflict() {
		if (merged_map == null)
			return -1;
		return merged_map.getConflictList().getNumberOfResolvedConflicts();
	}
	
	public int getNumberOfConflict() {
		if (merged_map == null)
			return -1;
		return merged_map.getConflictList().getList().size();
	}

	public void handleRetransmissionRequest(String sender,
			RequestRetransmissionMessageContent message_content) {
		SharedAction action = this.synchronous_editing_history.getMessage(
				message_content.original_sender, message_content.missing_message);
		if (action != null) {
			connection.resendCommand(action.getFrom(), action.getTimestamp().toString(), 
					test.marshall(action.getActionPair().getDoAction()), 
					test.marshall(action.getActionPair().getUndoAction()));
		}
		
	}

	public void sendRequestRetransmissionMessage(String from, int clock_value) {
		log.warn("request retransmission: " + from + " " + clock_value);
		connection.sendRequestRetransmission(from, clock_value);
	}

	public void clearHistory(VectorClock vector_clock) {
		this.synchronous_editing_history.clearHistory(vector_clock);
	}
	
	public void setChatColor(Color new_color) {
		this.chat_color = new_color;
	}
	
	// ---------------------------- gmomo methods ------------------------------------
	
	public void gmomoLogin(String uid, String pwd) throws Exception {
		gmomo_contact_list = new ContactList(this, uid);
		gmomo_connection.connect();
		gmomo_connection.login(uid, pwd, XMPP_RESOURCE_STRING);
		setupGmomoListener();
	}

	public boolean isGmomoAuthenticated() {
		return gmomo_connection.isConnected() &&
				gmomo_connection.isAuthenticated();
	}
	
	private void setupGmomoListener() {
		PacketListenerImpl gmomo_listener = new PacketListenerImpl();
		gmomo_listener.addPresenceListener(new PresenceListener(this));
		gmomo_listener.addInvitationListener(new InvitationListener(this));
//		gmomo_listener.addMessageListener(new GUIMessageListener(this));
		
		gmomo_connection.addPacketListener(gmomo_listener, null);
	}

	public void onGmomoContactRemoved(String address) {
		this.contact_list_window.removeContact(address);
	}

	public void onGmomoContactAdded(String address) {
		this.contact_list_window.addContact(address);
	}

	public void showGmomoLoginWindow() {
		plugins.sharedmind.view.gmomo.LoginWindow gmomo_login_window =
			new plugins.sharedmind.view.gmomo.LoginWindow(this.sharing_window, this);
	}
	
	public void showGmomoContactListWindow() {
		if (this.contact_list_window == null) {
			this.contact_list_window =
				new ContactListWindow(this, this.sharing_window);
		}
		this.contact_list_window.show();
	}

	public void invite(String address, String ip, int port) {
		Invitation invitation = new Invitation(address);
		invitation.setBody("Hi i hope to collaborate with you on SharedMind");
		System.out.println(ip);
		invitation.setHost(ip);
		invitation.setPort(port);
		this.gmomo_connection.sendPacket(invitation.getPacket());
	}

	public void onGmomoPresenceChanged(
			org.jivesoftware.smack.packet.Presence presence) {
		this.gmomo_contact_list.updateContact(presence);
	}

	public void onInvitationReceived(Invitation invitation) {
		new InvitationWindow(this.sharing_window, this, invitation);
	}
}
