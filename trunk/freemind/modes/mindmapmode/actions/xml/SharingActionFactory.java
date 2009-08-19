package freemind.modes.mindmapmode.actions.xml;

import java.util.Iterator;

import freemind.controller.Controller;
import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.FoldAction;

public class SharingActionFactory extends ActionFactory {
	private boolean propagate_folding_action;
	
	public SharingActionFactory(Controller c) {
		super(c);
		propagate_folding_action = false;
	}

	public void setPropagate_folding_action(boolean propagate_folding_action) {
		this.propagate_folding_action = propagate_folding_action;
	}

	public boolean remoteExecuteAction(ActionPair pair) {
		//synchronized (getController().getMapSharingController().getSynchronousEditingHistory()) {
			return super.executeAction(pair);
		//}
	}
	
	public boolean executeAction(ActionPair pair) {
		//synchronized (getController().getMapSharingController().getSynchronousEditingHistory()) {
		    if(pair == null)
		        return false;
		    boolean returnValue = true;
		    ActionPair filteredPair = pair;
		    if (controller.getMapSharingController()!=null) {
		    	boolean isFoldingAction = false;
		    	if (pair.getDoAction() instanceof CompoundAction &&
		    			((CompoundAction) pair.getDoAction()).getChoice(0) instanceof FoldAction)
		    		isFoldingAction = true;
		    	if (!isFoldingAction || propagate_folding_action) {
		    		controller.getMapSharingController().addnewAction(pair);
		    	}
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
			return returnValue;
		//}
	}
}
