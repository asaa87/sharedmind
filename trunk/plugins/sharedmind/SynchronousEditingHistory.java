package plugins.sharedmind;

import java.util.HashMap;
import java.util.Vector;

import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;

/**
 * 
 * @author asa
 * Contains the history of applied local and remote actions.
 *
 */
public class SynchronousEditingHistory {
    private Vector<SharedAction> history;
    private HashMap<String, SharedAction> last_action_of_participants;
    
	public SynchronousEditingHistory() {
        this.history = new Vector<SharedAction>();
        this.last_action_of_participants = new HashMap<String, SharedAction>();
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
		for (SharedAction action : possibleConflictingAction) {
			if (isConflicting(action, shared_action))
				return_value.add(action);
		}
		return return_value;
	}

	private boolean isConflicting(SharedAction action,
			SharedAction shared_action) {
		// Edit conflicting
		if (action.getActionPair().getDoAction() instanceof EditNodeAction && 
				shared_action.getActionPair().getDoAction() instanceof EditNodeAction &&
				((NodeAction) action.getActionPair().getDoAction()).getNode().equals(
						((NodeAction) shared_action.getActionPair().getDoAction()).getNode())) {
			return true;
		}
		return false;
	}
}
