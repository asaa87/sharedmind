package plugins.sharedmind;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import app.multicast.presence.PresenceMessage;

import freemind.main.XMLParseException;
import freemind.modes.attributes.Attribute;
import freemind.modes.attributes.AttributeRegistryElement;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.MindMapMapModel;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

public class Checkpoint {
	private static class CheckpointSuccessMessage {
		public String sender;
		public String vector_clock_string;
		public int version_number_candidate;
		public CheckpointSuccessMessage(String sender,
				String vector_clock_string, int version_number_candidate) {
			this.sender = sender;
			this.vector_clock_string = vector_clock_string;
			this.version_number_candidate = version_number_candidate;
		}
	}
	
    private static Logger logger = Logger.getLogger("plugins.Checkpoint");
    private static final String CHECKPOINT_PARTICIPANT_ATTRIBUTE = 
		"CHECKPOINT_PARTICIPANT";
    private static final String VERSION_ATTRIBUTE = "VERSION";
	private static final int UNVERSIONED = -1;
	
	private MapSharingController mpc;
	private Checkpoint previous_checkpoint;
	/**
	 * Checkpointed map and vector clock
	 */
	private MindMapMapModel map_model;
	private MindMapController map_controller;
	private File checkpointed_file;
	private VectorClock vector_clock;
	private String current_filename;
	private File parent_file;
	/**
	 * Contains local actions after checkpointing started.
	 */
	private Vector<SharedAction> pending_local_actions;

	/**
	 * last vector clock received in checkpoint success that is a possible checkpoint
	 */
	private VectorClock success_vector_clock;
	/**
	 * peers from whom we have received success message with the same vector clock as in
	 * success_vector_clock
	 */
	private HashMap<String, Boolean> success_message_received;
	private Vector<CheckpointSuccessMessage> delayed_success_message;
	private boolean generate_new_version_number;
	private int possible_version_number;
	
	public Checkpoint(MapSharingController mpc,
			          Checkpoint last_sucessfull_checkpoint) {
        this.mpc = mpc;
        this.previous_checkpoint = last_sucessfull_checkpoint;
        // TODO: fix when getFile return null
		current_filename = mpc.getController().getModel().getFile().getName();
		parent_file = mpc.getController().getModel().getFile().getParentFile();
		createCheckpointedMap();
        vector_clock = mpc.getMessageQueue().getVectorClock().clone();
        pending_local_actions = new Vector<SharedAction>();
        success_vector_clock = null;
        success_message_received = null;
        delayed_success_message = new Vector<CheckpointSuccessMessage>();
        logger.debug("Creating new checkpoint");
        // File is being shared for the first time, set the version
        logger.debug("VERSION: " + this.getVersion());
        if (this.getVersion() == UNVERSIONED) {
        	setVersion((int)(Math.random() * Integer.MAX_VALUE));
        	this.getParticipants().addValue(mpc.getNetworkUserId());
        	saveCheckpointToFile();
    		if (mpc.getController().getModel().getRegistry().getAttributes()
    				.containsElement(VERSION_ATTRIBUTE)) {
    			mpc.getController().getModel().getRegistry().getAttributes()
    				.getElement(VERSION_ATTRIBUTE).removeAllValues();
    		}
    		mpc.getController().getModel().getRegistry().getAttributes().registry(
    				new Attribute(VERSION_ATTRIBUTE, Integer.toString(this.getVersion())));
    		mpc.getController().getModel().getRegistry().getAttributes()
    				.getElement(VERSION_ATTRIBUTE).setRestriction(true);
    		mpc.getController().getModel().setSaved(false);
    		mpc.getController().save();
    		logger.debug("main map version: " + mpc.getController().getMap().getRegistry()
    				.getAttributes().getElement(VERSION_ATTRIBUTE).getValues().firstElement());
        }
        generate_new_version_number = false;
	}

	public void addRemoteActions(Vector<SharedAction> messages) {
		logger.debug("Add remote actions");
		for (SharedAction message : messages) {
			map_controller.getActionFactory().executeAction(message.getActionPair());
			logger.debug("local_vc: " + vector_clock.toString());
			logger.debug("timestamp: " + message.getTimestamp().toString());
			if (vector_clock.getHashMap().size() != message.getTimestamp().getHashMap().size())
				checkpointingFail();
			else
				vector_clock.adjustWithTimestamp(message.getTimestamp());
		}
		if (mpc.getMessageQueue().isEmpty()) {
			logger.debug("Sending checkpointing success message : " + vector_clock.toString());
			mpc.sendCheckpointingCompleteMessage(vector_clock.toString(), possible_version_number);
        }
	}
	
	public void addLocalAction(VectorClock timestamp, ActionPair pair) {
		pending_local_actions.add(new SharedAction(mpc.getNetworkUserId(), timestamp, pair));
	}
	
	/**
	 * Called when presence result come
	 * @param result
	 */
	public synchronized void onReceivePresenceResult(Vector<PresenceMessage> result) {
        logger.debug("Receive result from presence, start checkpointing");
        // Store participants
		AttributeRegistryElement current_participants = this.getParticipants();
		success_message_received = new HashMap<String, Boolean>();
		current_participants.removeAllValues();
		for (PresenceMessage message : result) {
			current_participants.addValue(message.getUserName());
			success_message_received.put(message.getUserName(), false);
		}
		
		// Decide checkpoint version
		boolean previous_participant_is_subset_of_current_participants = true;
		if (previous_checkpoint != null) {
			AttributeRegistryElement previous_participants = previous_checkpoint.getParticipants();
			Iterator iter = previous_participants.getValues().iterator();
			while (iter.hasNext()) {
			    if (!current_participants.getValues().contains((String)iter.next())) {
			    	previous_participant_is_subset_of_current_participants = false;
			    	break;
			    }
			}
		} else {
			previous_participant_is_subset_of_current_participants = false;
		}
		logger.debug("subset: " + previous_participant_is_subset_of_current_participants);
		if (previous_participant_is_subset_of_current_participants) {
		    possible_version_number = getVersion();
		    logger.debug("possible version number: " + possible_version_number);
		} else {
			generate_new_version_number = true;
			possible_version_number = (int)(Math.random() * Integer.MAX_VALUE);
		}
		
		// Check whether checkpointing is already successful or we have to wait
		// This peer is alone in the topic, no need to send checkpointing successful message
		if (this.getParticipants().getValues().getSize() == 1) {
			if (generate_new_version_number)
				this.setVersion(possible_version_number);
			onCheckpointingSuccess();
			return;
		}
		if (result.isEmpty()) {
			return;
		}
		boolean all_vc_is_the_same = true;
		Iterator<PresenceMessage> iter2 = result.iterator();
		PresenceMessage first_message = iter2.next();
		while (iter2.hasNext()) {
			if (!first_message.getVectorClock().equals(iter2.next().getVectorClock())) {
				all_vc_is_the_same = false;
				break;
			}
		}
		if (all_vc_is_the_same || mpc.getMessageQueue().isEmpty()) {
			mpc.sendCheckpointingCompleteMessage(this.vector_clock.toString(), possible_version_number);
		}
		for (CheckpointSuccessMessage message : delayed_success_message) {
			onReceiveCheckpointSuccessMessage(
					message.sender, message.vector_clock_string, possible_version_number);
		}
	}
	
	public synchronized void onReceiveCheckpointSuccessMessage(
			String sender, String vector_clock_string, int version_candidate) {
        logger.debug("Checkpoint success message: " + sender + " : " + vector_clock_string);
        // checkpointing is not started yet, delay message arrival
        if (success_message_received == null) {
        	delayed_success_message.add(
        			new CheckpointSuccessMessage(sender, vector_clock_string, version_candidate));
        	return;
        }
		VectorClock vc = new VectorClock(vector_clock_string);
		if (success_vector_clock == null) {
			success_message_received.put(sender, true);
			if (generate_new_version_number)
				setVersion(version_candidate);
		} else if (vc.getHashMap().equals(success_vector_clock.getHashMap())) {
			success_message_received.put(sender, true);
			if (generate_new_version_number)
				setVersion(Math.max(getVersion(), version_candidate));
		} else {
			// replace success vector clock with vc if all clock in success_vector_clock < vc
			boolean replace_success_vector_clock = true;
			for(Map.Entry<String, Integer> entry : vc) {
				if (entry.getValue() > success_vector_clock.getClock(entry.getKey())) {
					replace_success_vector_clock = false;
					break;
				}
			}
			logger.debug("replace vc: " + replace_success_vector_clock);
			if (replace_success_vector_clock) {
				success_vector_clock = vc;
				for(Map.Entry<String, Boolean> entry : success_message_received.entrySet())
					entry.setValue(false);
				success_message_received.put(sender, true);
				if (generate_new_version_number)
					setVersion(version_candidate);
			}
		}
		logger.debug("Check success: " + success_message_received.toString());
		boolean success = true;
		for(Map.Entry<String, Boolean> entry : success_message_received.entrySet()) {
			if (!entry.getValue()) {
				success = false;
				break;
			}
		}
		if (success)
			onCheckpointingSuccess();
	}
	
	private void createCheckpointedMap() {
		// Load checkpointed file into map_model
		try {
			mpc.getController().getModel().setSaved(false);
			mpc.getController().save();
			map_controller = (MindMapController)mpc.getController().getModel()
					.getModeController().getMode().createModeController();
			map_model = new MindMapMapModel(mpc.getController().getModel()
					.getFrame(), map_controller);
			map_model.load(mpc.getController().getModel().getURL());
			if (map_model.getRegistry().getAttributes().containsElement(
					CheckpointList.CHECKPOINT_LIST_ATTRIBUTE))
				map_model.getRegistry().getAttributes().getAttributeController()
					.performRemoveAttribute(CheckpointList.CHECKPOINT_LIST_ATTRIBUTE);
			if (map_model.getRegistry().getAttributes().containsElement(
					CheckpointList.LATEST_VERSION_ATTRIBUTE))
				map_model.getRegistry().getAttributes().getAttributeController()
					.performRemoveAttribute(CheckpointList.LATEST_VERSION_ATTRIBUTE);
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
	}
	
	public int getVersion() {
		return map_model.getRegistry().getAttributes().containsElement(VERSION_ATTRIBUTE) ?
				Integer.parseInt((String)map_model.getRegistry().getAttributes()
						.getElement(VERSION_ATTRIBUTE).getValues().firstElement()) : UNVERSIONED;
	}
	
	private void setVersion(int version) {
		if (map_model.getRegistry().getAttributes().containsElement(VERSION_ATTRIBUTE)) {
			map_model.getRegistry().getAttributes()
				.getElement(VERSION_ATTRIBUTE).removeAllValues();
		} else {
			map_model.getRegistry().getAttributes().registry(VERSION_ATTRIBUTE);
			map_model.getRegistry().getAttributes().getElement(VERSION_ATTRIBUTE).setRestriction(true);
		}
		map_model.getRegistry().getAttributes().getElement(VERSION_ATTRIBUTE)
			.addValue(Integer.toString(version));
	}
	
	private AttributeRegistryElement getParticipants() {
		if (!map_model.getRegistry().getAttributes().containsElement(CHECKPOINT_PARTICIPANT_ATTRIBUTE)) {
			map_model.getRegistry().getAttributes().registry(CHECKPOINT_PARTICIPANT_ATTRIBUTE);
			map_model.getRegistry().getAttributes().getElement(CHECKPOINT_PARTICIPANT_ATTRIBUTE).setRestriction(true);
		}
	    return map_model.getRegistry().getAttributes().getElement(CHECKPOINT_PARTICIPANT_ATTRIBUTE);
	}

	public void saveCheckpointToFile() {
		int dot_index = current_filename.lastIndexOf('.');
		System.out.println(current_filename);
//		dot_index = dot_index > 0 ? dot_index : current_filename.length();
		logger.debug("saving checkpoint version: " + this.getVersion());
		try {
			if (previous_checkpoint != null 
					&& previous_checkpoint.checkpointed_file != null 
					&& previous_checkpoint.getVersion() == this.getVersion()) {
				checkpointed_file = previous_checkpoint.checkpointed_file;
			} else {
				checkpointed_file = 
					File.createTempFile("CHECKPOINT_" + current_filename.substring(0, dot_index),
                        current_filename.substring(dot_index + 1),
                        parent_file);
				mpc.getCheckpointList().addNewCheckpoint(this.getVersion(), checkpointed_file.getAbsolutePath());
			}
	        map_model.save(checkpointed_file);
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}


	/**
	 * Called after a successful checkpointing
	 */
	private void onCheckpointingSuccess() {
		this.mpc.onVersionChange(this.getVersion());
		if (previous_checkpoint != null &&
				previous_checkpoint.getVersion() != this.getVersion()) {
			previous_checkpoint.saveCheckpointToFile();
			previous_checkpoint.previous_checkpoint = null;
		}
		if (mpc.getController().getModel().getRegistry().getAttributes()
				.containsElement(VERSION_ATTRIBUTE)) {
			mpc.getController().getModel().getRegistry().getAttributes()
				.getElement(VERSION_ATTRIBUTE).removeAllValues();
		}
		mpc.getController().getModel().getRegistry().getAttributes().registry(
				new Attribute(VERSION_ATTRIBUTE, Integer.toString(this.getVersion())));
		mpc.getController().getModel().getRegistry().getAttributes()
				.getElement(VERSION_ATTRIBUTE).setRestriction(true);
		mpc.getController().getModel().setSaved(false);
		mpc.getController().save();
		
		logger.debug("main map version: " + mpc.getController().getMap().getRegistry()
				.getAttributes().getElement(VERSION_ATTRIBUTE).getValues().firstElement());
        
		mpc.setLastSuccessfulCheckpoint(this);
        mpc.stopCheckpointing();
        // Publish pending local actions
		for (SharedAction entry : pending_local_actions) {
			mpc.sendLocalAction(entry.getTimestamp(), entry.getActionPair());
		}
		logger.debug("Checkpoint success: " + this.getVersion() + ": " + this.getVectorClock().toString());
	}
	
	public VectorClock getVectorClock() {
		return this.vector_clock;
	}

	public void checkpointingFail() {
        mpc.stopCheckpointing();
        // Publish pending local actions
		for (SharedAction entry : pending_local_actions) {
			mpc.sendLocalAction(entry.getTimestamp(), entry.getActionPair());
		}
        logger.debug("Checkpointing fail");
	}
}
