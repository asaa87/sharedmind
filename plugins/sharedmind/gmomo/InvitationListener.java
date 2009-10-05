package plugins.sharedmind.gmomo;

import plugins.sharedmind.MapSharingController;
import gmomo.packet.Invitation;

public class InvitationListener implements gmomo.InvitationListener {
	MapSharingController mpc;
	
	public InvitationListener(MapSharingController mpc) {
		this.mpc = mpc;
	}

	@Override
	public void invitationReceived(Invitation arg0) {
		mpc.onInvitationReceived(arg0);
	}

}
