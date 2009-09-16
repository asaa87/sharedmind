package plugins.sharedmind.synchronouscollaboration;

import java.util.HashMap;

import plugins.sharedmind.MapSharingController;

import freemind.modes.mindmapmode.MindMapController;

public class StoredVectorClock extends VectorClock {
	private static final String VECTOR_CLOCK_ATTRIBUTE = "VECTOR_CLOCK_ATTRIBUTE";
	private MindMapController mmController;

	public StoredVectorClock(MindMapController mmController) {
		super();
		this.mmController = mmController;
		if (mmController.getModel().getRegistry().getAttributes()
				.containsElement(VECTOR_CLOCK_ATTRIBUTE)) {
			String vector_string = (String) mmController.getModel()
					.getRegistry().getAttributes().getElement(
							VECTOR_CLOCK_ATTRIBUTE).getValues().firstElement();
			stringToVectorClock(vector_string);
		} else {
			// file is shared for the first time
			mmController.getModel().getRegistry().getAttributes().registry(
					VECTOR_CLOCK_ATTRIBUTE);
			super.addCollaborator(((MapSharingController)mmController.getController()
					.getMapSharingController()).getConnection().getUserName(),
					(int) (Math.random() * Integer.MAX_VALUE));
		}
		mmController.getModel().getRegistry().getAttributes().getElement(
				VECTOR_CLOCK_ATTRIBUTE).setRestriction(true);
		saveToFile();
	}
    
    public void incrementClock(String user_id) {
        super.incrementClock(user_id);
        saveToFile();
    }
    
    public void addCollaborator(String user_id, int clock) {
        super.addCollaborator(user_id, clock);
        saveToFile();
    }
    
    public void adjustWithTimestamp(VectorClock timestamp) {
        super.adjustWithTimestamp(timestamp);
        saveToFile();
    }
    
    private void saveToFile() {
		mmController.getModel().getRegistry().getAttributes().getElement(
				VECTOR_CLOCK_ATTRIBUTE).removeAllValues();
		mmController.getModel().getRegistry().getAttributes().getElement(
				VECTOR_CLOCK_ATTRIBUTE).addValue(this.vector_clock.toString());
		mmController.save();
    }
}
