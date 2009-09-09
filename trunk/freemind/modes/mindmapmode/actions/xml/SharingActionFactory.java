package freemind.modes.mindmapmode.actions.xml;

import java.util.Iterator;

import freemind.controller.Controller;
import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.DeleteNodeAction;
import freemind.controller.actions.generated.instance.FoldAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.modes.NodeAdapter;

public class SharingActionFactory extends ActionFactory {
	private boolean propagate_folding_action;
	
	public SharingActionFactory(Controller c) {
		super(c);
		propagate_folding_action = false;
	}

	public void setPropagate_folding_action(boolean propagate_folding_action) {
		this.propagate_folding_action = propagate_folding_action;
	}

	public synchronized boolean remoteExecuteAction(ActionPair pair) {
	    if (pair.getDoAction() instanceof NewNodeAction) {
	    	NewNodeAction new_node_action = (NewNodeAction) pair.getDoAction();
	    	NodeAdapter parent = controller.getMapSharingController().getController().getNodeFromID(new_node_action.getNode());
	    	new_node_action.setIndex(Math.min(new_node_action.getIndex(), parent.getChildCount()));
	    }
		return super.executeAction(pair);
	}
	
	public synchronized boolean executeAction(ActionPair pair) {
		//synchronized (getController().getMapSharingController().getSynchronousEditingHistory()) {
		    if(pair == null)
		        return false;
		    boolean returnValue = true;
		    ActionPair filteredPair = pair;
		    if (pair.getDoAction() instanceof NewNodeAction) {
		    	NewNodeAction new_node_action = (NewNodeAction) pair.getDoAction();
		    	NodeAdapter parent = controller.getMapSharingController().getController().getNodeFromID(new_node_action.getNode());
		    	new_node_action.setIndex(Math.min(new_node_action.getIndex(), parent.getChildCount()));
		    }
			// first filter:
			for (Iterator i = registeredFilters.iterator(); i.hasNext();) {
				ActionFilter filter = (ActionFilter) i.next();
				filteredPair = filter.filterAction(filteredPair);
			}
			
			// register for undo
			if(undoActionHandler != null)
			{
				try {
					undoActionHandler.executeAction(filteredPair);
				} catch (Exception e) {
					freemind.main.Resources.getInstance().logException(e);
					returnValue = false;
				}
			}
			
			Object[] aArray = registeredHandler.toArray();
			for (int i = 0; i < aArray.length; i++) {
	            ActionHandler handler = (ActionHandler) aArray[i];
				try {
	                handler.executeAction(filteredPair.getDoAction());
	            } catch (Exception e) {
	                freemind.main.Resources.getInstance().logException(e);
	                returnValue = false;
	                // to break or not to break. this is the question here...
	            }
			}
		    if (returnValue && controller.getMapSharingController()!=null) {
		    	boolean isFoldingAction = false;
		    	if (pair.getDoAction() instanceof CompoundAction &&
		    			((CompoundAction) pair.getDoAction()).getChoice(0) instanceof FoldAction)
		    		isFoldingAction = true;
		    	if (!isFoldingAction || propagate_folding_action) {
		    		controller.getMapSharingController().addnewAction(pair);
		    	}
		    }
			return returnValue;
		//}
	}
}
