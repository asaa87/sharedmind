package plugins.sharedmind.gmomo;

import org.jivesoftware.smack.packet.Presence;

import plugins.sharedmind.MapSharingController;

public class PresenceListener implements gmomo.PresenceListener {
	MapSharingController mpc;
	
	public PresenceListener(MapSharingController mpc) {
		this.mpc = mpc;
	}
	
	@Override
	public void presenceChanged(Presence arg0) {
		mpc.onGmomoPresenceChanged(arg0);
	}

}
