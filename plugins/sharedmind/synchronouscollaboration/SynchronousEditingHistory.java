package plugins.sharedmind.synchronouscollaboration;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import plugins.sharedmind.MapSharingController;
import plugins.sharedmind.helper.MapHelper;

import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.CutNodeAction;
import freemind.controller.actions.generated.instance.DeleteNodeAction;
import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.MoveNodesAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.controller.actions.generated.instance.PasteNodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.MindMapNode;
import freemind.modes.ModeController;
import freemind.modes.NodeAdapter;
import freemind.modes.NodeDownAction;
import freemind.modes.mindmapmode.actions.NodeUpAction;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

/**
 * 
 * @author asa
 * Contains the history of applied local and remote actions.
 *
 */
public class SynchronousEditingHistory {
    private static final PasteNodeAction PasteNodeAction = null;
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
		this.last_action_of_participants.put(action.getFrom(), action);
	}
	
	public synchronized void clearHistory (VectorClock vector_clock) {
		Vector<SharedAction> to_be_deleted = new Vector<SharedAction>();
		for (SharedAction shared_action : this.history) {
			if (shared_action.getTimestamp().happensBefore(vector_clock))
				to_be_deleted.add(shared_action);
		}
		
		this.history.removeAll(to_be_deleted);
	}
	
	private synchronized Vector<SharedAction> getPossiblyConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		for (SharedAction action : this.history) {
			if (action.getTimestamp().isConcurrent(shared_action.getTimestamp())) {
				return_value.add(action);
			}
		}
		return return_value;
	}
	
	public synchronized Vector<SharedAction> getConflictingChanges(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction> ();
		Vector<SharedAction> possibleConflictingAction = getPossiblyConflictingChanges(shared_action);
		log.info("Possible conflicting action: " + possibleConflictingAction.toString());
		for (SharedAction action : possibleConflictingAction) {
			if (isConflicting(action.getActionPair().getDoAction(), 
					shared_action.getActionPair().getDoAction()))
				return_value.add(action);
		}
		log.info("conflicting actions: " + return_value.toString());
		return return_value;
	}
	
    private Vector getSortedSiblings(MindMapNode node) {
        Vector nodes = new Vector();
        for (Iterator i = node.childrenUnfolded(); i.hasNext();) {
            nodes.add(i.next());
        }
        Collections.sort(nodes, new Comparator(){

            public int compare(Object o1, Object o2) {
                if (o1 instanceof MindMapNode) {
                    MindMapNode n1 = (MindMapNode) o1;
                    if (o2 instanceof MindMapNode) {
                        MindMapNode n2 = (MindMapNode) o2;
                        // left is less than right
                        int b1 = n1.isLeft()?0:1;
                        int b2 = n2.isLeft()?0:1;
                        return b1 - b2;
                    }
                }
                throw new IllegalArgumentException("Elements in LeftRightComparator are not comparable.");
            }
        });
        //logger.finest("Sorted nodes "+ nodes);
        return nodes;
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
		
		// Node up conflicting
		if (action instanceof MoveNodesAction &&
				shared_action instanceof MoveNodesAction) {
			MoveNodesAction move_action = (MoveNodesAction) action;
			MoveNodesAction shared_move_action = (MoveNodesAction) shared_action;
			
			ModeController mode_controller = mpc.getController();
			String local_parent = mode_controller.getNodeFromID(
					move_action.getNode()).getParentNode().getObjectId(mode_controller);
			String shared_parent = mode_controller.getNodeFromID(
					shared_move_action.getNode()).getParentNode().getObjectId(mode_controller);
			
			if (local_parent.equals(shared_parent)) {
				int local_direction = move_action.getDirection();
				int shared_direction = shared_move_action.getDirection();
				
				for (int i = 0; i < move_action.sizeNodeListMemberList(); ++i) {
					String local_node = move_action.getNodeListMember(i).getNode();
					for (int j = 0; j < shared_move_action.sizeNodeListMemberList(); ++j) {
						String shared_node = shared_move_action.getNodeListMember(j).getNode();
						
						// Same node is moved twice
						if (local_node.equals(shared_node)) {
							return true;
						}
						
						NodeAdapter parent_node = mode_controller.getNodeFromID(local_parent);
						int child_count = parent_node.getChildCount();
						int new_local_node_index = 
							parent_node.getIndex(mode_controller.getNodeFromID(local_node));
						int local_node_index = 
							(new_local_node_index - local_direction) % child_count;
						int shared_node_index = 
							parent_node.getIndex(mode_controller.getNodeFromID(shared_node));
						int new_shared_node_index = 
							(shared_node_index + shared_direction) % child_count;
						
						if (local_node_index == new_shared_node_index ||
								shared_node_index == new_local_node_index) {
							return true;
						}
					}
				}
			}
		}
		
		boolean shared_is_delete = shared_action instanceof DeleteNodeAction 
				|| shared_action instanceof CutNodeAction;
		boolean local_is_delete = action instanceof DeleteNodeAction 
				|| action instanceof CutNodeAction;
		
		// Shared action delete subtree that is modified in action
		if (shared_is_delete && !(local_is_delete)) {
			log.debug("remote action is delete");
			
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
			
			// node in shared node is already deleted
			try {
				shared_node = mpc.getController().getNodeFromID(((NodeAction) shared_action).getNode());
			} catch (IllegalArgumentException e) {
				return true;
			}
			
			if (MapHelper.isDescendant(shared_node, local_node))
				return true;
		}
		
		if (local_is_delete && !shared_is_delete) {
			log.debug("local action is delete");
			
			// Shared action modifies a node that has been deleted
			if (((NodeAction) action).getNode().equals(
					((NodeAction) shared_action).getNode()))
					return true;
			
			NodeAdapter remote_node;
			try {
				remote_node = mpc.getController().getNodeFromID(((NodeAction)shared_action).getNode());
			} catch (IllegalArgumentException e) {
				return true;
			}
			
			try {
				NodeAdapter local_node = mpc.getController().getNodeFromID(((NodeAction) action).getNode());
				if (shared_action instanceof NewNodeAction &&
						MapHelper.isDescendant(local_node, remote_node)) {
					return true;
					
				}
			} catch (IllegalArgumentException e) {	}
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
	
	/*
	 * Returns the actions that needed to be undone before the shared action is executed 
	 * and redone after that. These actions are:
	 * 1. Concurrent actions of delete and new node, where the node deleted is the sibling of new node.
	 * 2. Concurrent new node actions, where the parent of these new nodes are the same.
	 */
	public synchronized Vector<SharedAction> getFollowingActions(SharedAction shared_action) {
		Vector<SharedAction> return_value = new Vector<SharedAction>();
		ActionPair action_pair = shared_action.getActionPair();
		
		if (action_pair.getDoAction() instanceof NodeAction ||
				action_pair.getDoAction() instanceof CompoundAction) {
			
			// Get list of actions in history that might need to be undone first.
			int i;
			for (i = 0; i < history.size(); ++i){
				if (this.history.get(i).compareTo(shared_action) > 0) {
					break;
				}	
			}
			Vector<SharedAction> following_actions = 
					new Vector<SharedAction>(this.history.subList(i, this.history.size()));
			
			HashMap<String, String> new_nodes = new HashMap<String, String>();
			if (action_pair.getDoAction() instanceof NewNodeAction) {
				NewNodeAction new_node = (NewNodeAction) action_pair.getDoAction();
				new_nodes.put(new_node.getNewId(), new_node.getNode());
			}
			for (SharedAction action : following_actions) {
				XmlAction do_action = action.getActionPair().getDoAction();
				if (do_action instanceof NewNodeAction) {
					NewNodeAction new_node = (NewNodeAction) do_action;
					new_nodes.put(new_node.getNewId(), new_node.getNode());
				}
			}
			
			for (SharedAction action : following_actions) {
				if (!action.isUndoed() && needUndo(action.getActionPair(), new_nodes)) {
					return_value.add(action);
				}
			}
		}
		return return_value;
	}
	
	private boolean needUndo(ActionPair pair, HashMap<String, String> new_nodes) {
		// Get list of parents
		Vector<String> parent_list = getListOfParents(pair);
		
		for (String parent : parent_list) {
			if (new_nodes.containsValue(parent))
				return true;
		}
		
		if (parent_list.size() == 0 && pair.getDoAction() instanceof NodeAction) {
			NodeAction node_action = (NodeAction) pair.getDoAction();
			if (new_nodes.containsKey(node_action.getNode()))
				return true;
		}
		return false;
	}
	
	private Vector<String> getListOfParents (ActionPair pair) {
		Vector<String> return_value = new Vector<String>();
		
		if (pair.getDoAction() instanceof NewNodeAction) {
			NewNodeAction do_action = (NewNodeAction) pair.getDoAction();
			return_value.add(do_action.getNode());
		} else if (pair.getDoAction() instanceof DeleteNodeAction ||
				pair.getDoAction() instanceof CutNodeAction) {
			PasteNodeAction undo_action = (PasteNodeAction) pair.getUndoAction();
			return_value.add(undo_action.getNode());
		} else if (pair.getDoAction() instanceof CompoundAction &&
				((CompoundAction) pair.getDoAction()).getChoice(0) instanceof CutNodeAction) {
			List<PasteNodeAction> undo_actions = 
				((CompoundAction) pair.getUndoAction()).getListChoiceList();
			for (PasteNodeAction undo_action : undo_actions) {
				return_value.add(undo_action.getNode());
			}
		}
		
		return return_value;
	}
	
	public synchronized Vector<SharedAction> getUndoedChanges() {
		Vector<SharedAction> return_value = new Vector<SharedAction>();
		for (SharedAction action : history) {
			if (action.isUndoed()) {
				return_value.add(action);
			}
		}
		return return_value;
	}

	public synchronized SharedAction getMessage(String original_sender, int missing_message) {
		for (SharedAction action : this.history) {
			if (action.getFrom().equals(original_sender) &&
					action.getTimestamp().getClock(original_sender) == missing_message)
				return action;
		}
		return null;
	}
}
