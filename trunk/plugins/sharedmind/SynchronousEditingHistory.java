package plugins.sharedmind;

import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.ws.jaxme.logging.Log4jLoggerFactory;

import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.CutNodeAction;
import freemind.controller.actions.generated.instance.DeleteNodeAction;
import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.NodeAdapter;
import freemind.modes.mindmapmode.actions.NewChildAction;
import freemind.modes.mindmapmode.actions.xml.ActorXml;

/**
 * 
 * @author asa
 * Contains the history of applied local and remote actions.
 *
 */
public class SynchronousEditingHistory {
    private Vector<SharedAction> history;
    private HashMap<String, SharedAction> last_action_of_participants;
    private MapSharingController mpc;
    private Logger log;
    
	public SynchronousEditingHistory(MapSharingController mpc) {
		log = Logger.getLogger(this.getClass());
        this.history = new Vector<SharedAction>();
        this.last_action_of_participants = new HashMap<String, SharedAction>();
        this.mpc = mpc;
	}
	
	public synchronized void addToHistory(SharedAction action) {
		int i;
		for (i = 0; i < history.size(); ++i){
			if (this.history.get(i).compareTo(action) > 0) {
				break;
			}	
		}
		log.warn("insert to history: " + i + " " + history.size());
		this.history.insertElementAt(action, i);
		
		Vector<String> participants = mpc.getMessageQueue().getCurrentParticipant();
		
		SharedAction minimal = action;
		boolean change_minimal = true;
		log.warn(participants.toString());
		for (String user_id : participants) {
			if (!this.last_action_of_participants.containsKey(user_id)) {
				change_minimal = false;
				break;
			}
			SharedAction shared_action = this.last_action_of_participants.get(user_id);
			if (shared_action.compareTo(minimal) < 0)
				minimal = shared_action;
		}
		
		if (change_minimal && action.getFrom().equals(minimal.getFrom())) {
			int minimal_index = this.history.indexOf(minimal);
			this.history = new Vector<SharedAction>(this.history.subList(minimal_index, this.history.size()));
		}
		
		this.last_action_of_participants.put(action.getFrom(), action);
	}
	
	private synchronized Vector<SharedAction> getPossiblyConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		for (int i = 0; i < this.history.size(); ++i) {
			if (this.history.get(i).getTimestamp().isConcurrent(shared_action.getTimestamp())) {
				return_value = new Vector<SharedAction> (this.history.subList(i, this.history.size()));
				break;
			}
		}
		return return_value;
	}
	
	public synchronized Vector<SharedAction> getConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		Vector<SharedAction> possibleConflictingAction = getPossiblyConflictingChanges(shared_action);
		log.warn("Possible conflicting action: " + possibleConflictingAction.toString());
		for (SharedAction action : possibleConflictingAction) {
			if (isConflicting(action.getActionPair().getDoAction(), 
					shared_action.getActionPair().getDoAction()))
				return_value.add(action);
		}
		log.warn("conflicting actions: " + return_value.toString());
		return return_value;
	}
	
	private boolean isConflicting(XmlAction action, XmlAction shared_action) {
		if (action instanceof CompoundAction) {
			CompoundAction compound = (CompoundAction) action;
			return isConflicting(compound, shared_action);
		}
		
		if (shared_action instanceof CompoundAction) {
			CompoundAction compound = (CompoundAction) shared_action;
			return isConflicting(action, compound);
		}
		
		// Edit conflicting
		if (action instanceof EditNodeAction && 
				shared_action instanceof EditNodeAction &&
				((NodeAction) action).getNode().equals(
						((NodeAction) shared_action).getNode())) {
			return true;
		}
		
		boolean shared_is_delete = shared_action instanceof DeleteNodeAction 
				|| shared_action instanceof CutNodeAction;
		boolean local_is_delete = action instanceof DeleteNodeAction 
				|| action instanceof CutNodeAction;
		
		// Shared action delete subtree that is modified in action
		if (shared_is_delete && !(local_is_delete)) {
			log.warn("remote action is delete");
			
			// node deleted by shared action is modified in local
			if (((NodeAction) shared_action).getNode().equals(((NodeAction) action).getNode()))
				return true;
			
			NodeAdapter local_node, shared_node;
			
			// node is deleted in local
			try {
				local_node = mpc.getController().getNodeFromID(((NodeAction) action).getNode());
			} catch (IllegalArgumentException e) {
				return false;
			}
			
			// node is deleted in shared
			try {
				shared_node = mpc.getController().getNodeFromID(((NodeAction) shared_action).getNode());
			} catch (IllegalArgumentException e) {
				return true;
			}
			
			if (MapHelper.isDescendant(shared_node, local_node))
				return true;
		}
		
		// Shared action modifies a node that has been deleted
		if (local_is_delete && !shared_is_delete) {
			log.warn("local action is delete");
			try {
				NodeAdapter remote_node = mpc.getController().getNodeFromID(((NodeAction)shared_action).getNode());
				if (shared_action instanceof NewNodeAction &&
						remote_node.getChildCount() < ((NewNodeAction)shared_action).getIndex()) {
					return true;
					
				}
			} catch (IllegalArgumentException e) {
				return true;
			}
			if (((NodeAction) action).getNode().equals(
					((NodeAction) shared_action).getNode()))
					return true;
		}
		return false;
	}
	
	private boolean isConflicting(CompoundAction action, XmlAction shared_action) {
		Object[] actions = action.getListChoiceList().toArray();
		for (int i = 0; i < actions.length; i++) {
		    Object obj = actions[i];
		    if (obj instanceof XmlAction
		    		&& isConflicting((XmlAction) obj, shared_action)) {
    			return true;
            }
        }
		return false;
	}
	
	private boolean isConflicting(XmlAction action, CompoundAction shared_action) {
		Object[] actions = shared_action.getListChoiceList().toArray();
		for (int i = 0; i < actions.length; i++) {
		    Object obj = actions[i];
		    if (obj instanceof XmlAction &&
		    		isConflicting(action, (XmlAction) obj)) {
    				return true;
            }
        }
		return false;
	}
}
