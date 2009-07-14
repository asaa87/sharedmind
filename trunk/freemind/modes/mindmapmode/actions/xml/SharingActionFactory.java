package freemind.modes.mindmapmode.actions.xml;

import java.util.Iterator;

import freemind.controller.Controller;

public class SharingActionFactory extends ActionFactory {

	public SharingActionFactory(Controller c) {
		super(c);
	}

	public boolean remoteExecuteAction(ActionPair pair) {
		return super.executeAction(pair);
	}
	
	public boolean executeAction(ActionPair pair) {
	    if(pair == null)
	        return false;
	    boolean returnValue = true;
	    ActionPair filteredPair = pair;
	    if (controller.getMapSharingController()!=null)
	    	controller.getMapSharingController().addnewAction(pair);
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
	}
}
