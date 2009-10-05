package plugins.sharedmind.gmomo;

import java.util.HashMap;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.util.StringUtils;

import plugins.sharedmind.MapSharingController;

public class ContactList {
	private MapSharingController mpc;
	private HashMap<String, Contact> list;
	private String user_id;
	
	public ContactList(MapSharingController mpc, String user_id) {
		this.mpc = mpc;
		this.list = new HashMap<String, Contact>();
		this.user_id = user_id.toLowerCase();
	}
	
	public synchronized void updateContact(Presence presence) {
		String xmppAddress = presence.getFrom();
		String address = StringUtils.parseBareAddress(xmppAddress).toLowerCase();
		String resource = StringUtils.parseResource(xmppAddress);
		String name = StringUtils.parseName(xmppAddress).toLowerCase();
		Type type = presence.getType();
		
		if (name.equals(user_id))
			return;
		if (list.containsKey(address)) {
			if (type == Type.unavailable) {
				if (list.get(address).tryRemoveResource(resource) == 0) {
					list.remove(address);
					mpc.onGmomoContactRemoved(address);
				}
			} else if (type == Type.available) {
				list.get(address).addResource(resource);
			}
		}else {
			if ( type == Type.available ) {
				Contact contact = new Contact(xmppAddress);
				this.list.put(address, contact);
				mpc.onGmomoContactAdded(address);
			}
		}
	}
}
