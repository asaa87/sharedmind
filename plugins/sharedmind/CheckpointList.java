package plugins.sharedmind;

public class CheckpointList extends ReadOnlyCheckpointList {
    private MapSharingController mpc;
	
	public CheckpointList(MapSharingController mpc) {
		super(mpc.getController());
		this.mpc = mpc;
	}
	
	public void addNewCheckpoint(int version, String filename) {
		if (version == latest_version)
			return;
		CheckpointListAttributeValue checkpoint_list_attribute_value = 
			new CheckpointListAttributeValue(version, latest_version, filename);
		version_to_checkpoint.put(version, checkpoint_list_attribute_value);
		version_to_previous_version.put(version, latest_version);
		latest_version = version;
		if (!mpc.getController().getModel().getRegistry().getAttributes()
				.getElement(CHECKPOINT_LIST_ATTRIBUTE).getValues()
				.contains(checkpoint_list_attribute_value.toString())) {
			mpc.getController().getModel().getRegistry().getAttributes()
				.getElement(CHECKPOINT_LIST_ATTRIBUTE)
				.addValue(checkpoint_list_attribute_value.toString());
			mpc.getController().getModel().getRegistry().getAttributes()
				.getElement(CHECKPOINT_LIST_ATTRIBUTE).save();
		}
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).removeAllValues();
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).addValue(Integer.toString(version));
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).save();
	}
	
	public void removeLatestCheckpoint() {
		int temp = latest_version;
		latest_version = version_to_previous_version.get(temp);
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).removeAllValues();
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).addValue(Integer.toString(latest_version));
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(LATEST_VERSION_ATTRIBUTE).save();
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(CHECKPOINT_LIST_ATTRIBUTE).removeValue(
					version_to_previous_version.get(temp).toString());
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(CHECKPOINT_LIST_ATTRIBUTE).save();
		version_to_previous_version.remove(temp);
		version_to_checkpoint.remove(temp);
	}
	
	public void changeCheckpointFileName(int version, String filename) {
		CheckpointListAttributeValue old = version_to_checkpoint.get(version);
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(CHECKPOINT_LIST_ATTRIBUTE).removeValue(old.toString());
		old.setFilename(filename);
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(CHECKPOINT_LIST_ATTRIBUTE).addValue(old.toString());
		mpc.getController().getModel().getRegistry().getAttributes()
			.getElement(CHECKPOINT_LIST_ATTRIBUTE).save();
	}
}
