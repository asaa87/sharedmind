package plugins.sharedmind;

import java.util.HashMap;
import java.util.Iterator;

import freemind.modes.mindmapmode.MindMapController;

public class ReadOnlyCheckpointList {
    public static final String CHECKPOINT_LIST_ATTRIBUTE = "CHECKPOINT_LIST";
    public static final String LATEST_VERSION_ATTRIBUTE = "LATEST_VERSION";
    protected static final int NO_CHECKPOINT_VERSION = 0;
    
    protected HashMap<Integer, CheckpointListAttributeValue> version_to_checkpoint;
	protected HashMap<Integer, Integer> version_to_previous_version;
	protected int latest_version;
	
	public ReadOnlyCheckpointList(MindMapController mmController) {
		version_to_checkpoint = new HashMap<Integer, CheckpointListAttributeValue>();
		version_to_previous_version = new HashMap<Integer, Integer>();
		
		if (mmController.getModel().getRegistry().getAttributes()
			.containsElement(CHECKPOINT_LIST_ATTRIBUTE)) {
			Iterator iter = mmController.getModel().getRegistry()
				.getAttributes().getElement(CHECKPOINT_LIST_ATTRIBUTE).getValues().iterator();
			while (iter.hasNext()) {
				CheckpointListAttributeValue current = 
					new CheckpointListAttributeValue((String)iter.next());
				version_to_checkpoint.put(current.getVersion(), current);
				version_to_previous_version.put(current.getVersion(), current.getPreviousVersion());
			}
		} else {
			mmController.getModel().getRegistry().getAttributes()
				.registry(CHECKPOINT_LIST_ATTRIBUTE);
			mmController.getModel().getRegistry().getAttributes()
				.getElement(CHECKPOINT_LIST_ATTRIBUTE).setRestriction(true);
		}
		if (mmController.getModel().getRegistry().getAttributes()
				.containsElement(LATEST_VERSION_ATTRIBUTE)) {
			latest_version = Integer.parseInt((String)mmController.getModel().getRegistry()
					.getAttributes().getElement(LATEST_VERSION_ATTRIBUTE).getValues().firstElement());
		} else {
			mmController.getModel().getRegistry().getAttributes()
				.registry(LATEST_VERSION_ATTRIBUTE);
			mmController.getModel().getRegistry().getAttributes()
				.getElement(LATEST_VERSION_ATTRIBUTE).setRestriction(true);
			latest_version = NO_CHECKPOINT_VERSION;
			mmController.getModel().getRegistry().getAttributes()
				.getElement(LATEST_VERSION_ATTRIBUTE).addValue(Integer.toString(latest_version));
		}
	}
	
	public int getLatestCheckpointVersion() {
		return latest_version;
	}
	
	public int getPreviousCheckpointVersion(int version) {
		return version_to_previous_version.get(version);
	}
	
	public String getCheckpointedFileName(int version) {
		return version_to_checkpoint.get(version).getFilename();
	}
	
	public HashMap<Integer, String> getCheckpointFileList() {
		HashMap<Integer, String> return_value = new HashMap<Integer, String>();
		for (CheckpointListAttributeValue entry : version_to_checkpoint.values()) {
			return_value.put(entry.getVersion(), entry.getFilename());
		}
		return return_value;
	}
}
