package plugins;

import freemind.extensions.ModeControllerHookAdapter;
import freemind.modes.mindmapmode.MindMapController;

public class MapSharingControllerHook extends ModeControllerHookAdapter {
	   public void startupMapHook() {
	        super.startupMapHook();
	        MapSharingController internalcontroller = new MapSharingController((MindMapController) getController());
	        
	            }

}
