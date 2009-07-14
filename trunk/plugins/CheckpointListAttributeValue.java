package plugins;

public class CheckpointListAttributeValue {
	private int version;
	private int previous_version;
	private String filename;
	
	public CheckpointListAttributeValue(int version, int previous_version, String filename) {
		this.version = version;
		this.previous_version = previous_version;
		this.setFilename(filename);
	}
	
	public CheckpointListAttributeValue(String object_string) {
		String temp[] = object_string.substring(1, object_string.length() - 1).split(",");
		this.version = Integer.parseInt(temp[0]);
		this.previous_version = Integer.parseInt(temp[1]);
		this.setFilename(temp[2]);
	}
	
	public int getVersion() {
		return version;
	}
	
	public int getPreviousVersion() {
		return previous_version;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String toString() {
		return "[" + version + "," + previous_version + "," + getFilename() + "]";
	}
}
