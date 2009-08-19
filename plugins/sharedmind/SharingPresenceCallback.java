package plugins.sharedmind;

import java.util.HashMap;
import java.util.Vector;

import app.multicast.presence.PresenceCallback;
import app.multicast.presence.PresenceMessage;

public class SharingPresenceCallback implements PresenceCallback {
    Connection connection;
	private MapSharingController mpc;
    
    public SharingPresenceCallback(
    		MapSharingController mpc, Connection connection) {
    	this.mpc = mpc;
    	this.connection = connection;
    }
    
	public String getUserName() {
		return connection.getUserName();
	}

	public HashMap<String, Integer> getVectorClock() {
		return new HashMap<String, Integer>(
				mpc.getCheckpointInProgress().getVectorClock().getHashMap());
	}

	public int getVersion() {
		return mpc.getCheckpointInProgress().getVersion();
	}

	public void onInitiatePresence() {
		if (mpc.getCheckpointInProgress() != null) {
			mpc.getCheckpointInProgress().checkpointingFail();
		}
		mpc.startCheckpointing();
	}

	public void onReceivePresenceResult(Vector<PresenceMessage> result) {
		mpc.getCheckpointInProgress().onReceivePresenceResult(result);
		Vector<String> participants = new Vector<String>();
		for (PresenceMessage message : result) {
			participants.add(message.getUserName());
		}
		mpc.getMessageQueue().setCurrentParticipant(participants);
		mpc.updateOnlineUserList(participants);
	}

	@Override
	public void onConnected() {
		mpc.hideConnectingWindow();
		mpc.showGetMapWindow();
	}

}
