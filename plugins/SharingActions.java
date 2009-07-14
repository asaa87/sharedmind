package plugins;

import org.apache.log4j.Logger;

import pl.edu.pjwstk.mteam.core.DHTObject;
import pl.edu.pjwstk.mteam.core.Node;
import pl.edu.pjwstk.mteam.core.NodeCallback;
import pl.edu.pjwstk.mteam.core.NodeError;

public class SharingActions implements NodeCallback{
    private static Logger logger = Logger.getLogger("plugins.SharingActions");
    private MapSharingController mpc;
    
	public SharingActions(MapSharingController mpc) {
		super();
		this.mpc = mpc;
	}

	public void onDisconnect(Node node) {
		logger.debug(mpc.getNetworkUserId() + ": Disconnect");
	}

	public void onInsertObject(Node node, DHTObject object) {
		logger.debug(mpc.getNetworkUserId() + ": Insert object successful");
	}

	public void onJoin(Node node) {
		logger.debug(mpc.getNetworkUserId() + ": Join Successful");
    	mpc.initializeMessageQueue();
    	mpc.addSharingWindow();
	}

	public void onObjectLookup(Node node, Object object) {
		logger.debug(mpc.getNetworkUserId() + ": Object lookup successful:" + object.toString());
	}

	public void onOverlayError(Node node, Object sourceID, int errorCode) {
		String error_message = "";
		switch (errorCode) {
		case NodeError.BOOTSTRAPERR:
			error_message = "Bootstrap Error";
			break;
		case NodeError.INSERTERR:
			error_message = "Insert Error";
			break;
		case NodeError.RLOOKUPERR:
			error_message = "RLookup Error";
			break;
		case NodeError.ULOOKUPERR:
			error_message = "ULookup Error";
			break;
		}
		logger.debug(mpc.getNetworkUserId() + ": " + error_message);
		mpc.showCommunicationError(error_message);
	}

	public void onPubSubError(Node node, Object topicID, int errorCode) {
		String error_message = "";
		switch (errorCode) {
		case NodeError.TOPICEXISTSERR:
			error_message = "Topic already exists";
			break;
		case NodeError.AUTHENTICATIONERR:
			error_message = "Authentication error";
			break;
		case NodeError.NOSUCHTOPICERR:
			error_message = "No such topic";
			break;
		}
		logger.debug(mpc.getNetworkUserId() + ": " + error_message);
		mpc.showCommunicationError(error_message);
	}

	public void onTopicCreate(Node node, Object topicID) {
		logger.debug(mpc.getNetworkUserId() + ": Create topic successful:" + topicID.toString());
	}

	public void onTopicNotify(Node node, Object topicID, Object message) {
		logger.debug(mpc.getNetworkUserId() + ": Topic notify successful:" +
				     topicID.toString());
		mpc.processMessage((String) message);
	}
	
	public void onTopicRemove(Node node, Object topicID) {
		logger.debug(mpc.getNetworkUserId() + ": Topic remove successful:" + topicID.toString());
	}

	public void onTopicSubscribe(Node node, Object topicID) {
		logger.debug(mpc.getNetworkUserId() + ": Topic subscribe successful:" + topicID.toString());
		mpc.setTopic((String) topicID);
		mpc.showGetMapWindow();
	}

	public void onUserLookup(Node node, Object userInfo) {
		logger.debug(mpc.getNetworkUserId() + ": User lookup successful:" + userInfo.toString());
	}

}
