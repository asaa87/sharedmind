package plugins.sharedmind.gmomo;

import java.util.Vector;

import org.jivesoftware.smack.util.StringUtils;

public class Contact {
	private String address;
	private Vector<String> resources;
	
	public Contact(String xmppAddress) {
		this.address = StringUtils.parseBareAddress(xmppAddress);
		this.resources = new Vector<String>();
		this.resources.add(StringUtils.parseServer(xmppAddress));
	}
	
	public void addResource(String resource) {
		if (!resources.contains(resource))
			resources.add(resource);
	}
	
	public int tryRemoveResource(String resource) {
		resources.remove(resource);
		return resources.size();
	}
	
	public boolean equals(Contact contact) {
		if (this == contact)
			return true;
		if (contact == null)
			return false;
		return contact.address.equals(this.address);
	}
	
	public String toString() {
		return address;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public String getName() {
		return StringUtils.parseName(address);
	}
	
	public String getServer() {
		return StringUtils.parseServer(address);
	}
	
	public int getResourceCount() {
		return resources.size();
	}
}
