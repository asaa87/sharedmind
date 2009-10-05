package plugins.sharedmind.view.gmomo;

import gmomo.packet.Invitation;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import plugins.sharedmind.MapSharingController;

public class InvitationWindow {

	public InvitationWindow(JFrame owner, MapSharingController mpc, Invitation invitation) {
	    String[] options = { "Yes", "No" };
	    String question = "You've received an invitation from: " + invitation.getFrom() +
	    	"\n Do you want to collaborate?";
	    	
	    	int result = JOptionPane.showOptionDialog(owner, question, 
	    			"Invitation from " + invitation.getFrom(),
	    			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
	    			null, options, options[0]);
	    	
	    	if (result == 0) {
	    		try {
	    			mpc.unsubscribeToTopic();
	    		} catch (Exception e) {
	    			
	    		}
	    		mpc.subscribeToTopic(invitation.getHost() + ":" + invitation.getPort());
	    	}
	}

}
