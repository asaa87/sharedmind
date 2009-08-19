package freemind.modes.mindmapmode;

import freemind.modes.ModeController;

public interface MergedMapInterface {

	ModeController getMergedMap();

	void addNodeToMergedMap(String objectId, String newId);

}