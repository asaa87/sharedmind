package plugins.sharedmind;

import java.util.HashMap;
import java.util.Vector;

import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.CutNodeAction;
import freemind.controller.actions.generated.instance.DeleteNodeAction;
import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.NodeAdapter;
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
    
	public SynchronousEditingHistory(MapSharingController mpc) {
        this.history = new Vector<SharedAction>();
        this.last_action_of_participants = new HashMap<String, SharedAction>();
        this.mpc = mpc;
	}
	
	public void addToHistory(SharedAction action) {
		int i;
		for (i = 0; i < history.size(); ++i){
			if (this.history.get(i).compareTo(action) > 0) {
				break;
			}	
		}
		this.history.insertElementAt(action, i);
		
		SharedAction minimal = action;
		for (SharedAction shared_action : this.last_action_of_participants.values()) {
			if (shared_action.compareTo(minimal) < 0)
				minimal = shared_action;
		}
		
		if (minimal.getFrom().equals(action.getFrom())) {
			int minimal_index = this.history.indexOf(minimal);
			this.history = new Vector<SharedAction>(this.history.subList(minimal_index, this.history.size()));
		}
		
		this.last_action_of_participants.put(action.getFrom(), action);
	}
	
	public Vector<SharedAction> getPossiblyConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		for (int i = 0; i < this.history.size(); ++i) {
			if (this.history.get(i).getTimestamp().isConcurrent(shared_action.getTimestamp())) {
				return_value = new Vector<SharedAction> (this.history.subList(i, this.history.size()));
				break;
			}
		}
		return return_value;
	}
	
	public Vector<SharedAction> getConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		Vector<SharedAction> possibleConflictingAction = getPossiblyConflictingChanges(shared_action);
		System.out.println("Possible conflicting action: " + possibleConflictingAction.toString());
		for (SharedAction action : possibleConflictingAction) {
			if (isConflicting(action.getActionPair().getDoAction(), 
					shared_action.getActionPair().getDoAction()))
				return_value.add(action);
		}
		System.out.println("conflicting actions: " + return_value.toString());
		return return_value;
	}
	
	private boolean isConflicting(XmlAction action, XmlAction shared_action) {
		if (action instanceof CompoundAction) {
			CompoundAction compound = (CompoundAction) action;
			Object[] actions = compound.getListChoiceList().toArray();
			for (int i = 0; i < actions.length; i++) {
			    Object obj = actions[i];
			    if (obj instanceof XmlAction) {
	                XmlAction xmlAction = (XmlAction) obj;
	    			return isConflicting(xmlAction, shared_action);
	            }
	        }
		}
		
		if (shared_action instanceof CompoundAction) {
			CompoundAction compound = (CompoundAction) shared_action;
			Object[] actions = compound.getListChoiceList().toArray();
			for (int i = 0; i < actions.length; i++) {
			    Object obj = actions[i];
			    if (obj instanceof XmlAction) {
	                XmlAction xmlAction = (XmlAction) obj;
	    			return isConflicting(action, xmlAction);
	            }
	        }
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
			if (((NodeAction) shared_action).getNode().equals(((NodeAction) action).getNode()))
				return true;
			try {
				if (MapHelper.isDescendant(
					mpc.getController().getNodeFromID(((NodeAction) shared_action).getNode()), 
					mpc.getController().getNodeFromID(((NodeAction) action).getNode())))
						return true;
			} catch (IllegalArgumentException e) {
				return true;
			}
		}
		
		// Shared action modifies a node that has been deleted
		if (local_is_delete && !shared_is_delete) {
			try {
				mpc.getController().getNodeFromID(((NodeAction)shared_action).getNode());
			} catch (IllegalArgumentException e) {
				return true;
			}
		}
		return false;
	}
}
