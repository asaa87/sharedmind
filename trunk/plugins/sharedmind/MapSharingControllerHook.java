package plugins.sharedmind;

import freemind.extensions.ModeControllerHookAdapter;
import freemind.modes.mindmapmode.MapSharingControllerInterface;
import freemind.modes.mindmapmode.MindMapController;

public class MapSharingControllerHook extends ModeControllerHookAdapter {
	   public void startupMapHook() {
	        super.startupMapHook();
	        MapSharingControllerInterface internalcontroller = new MapSharingController((MindMapController) getController());
	        
	            }

}
