package plugins.sharedmind.view;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import plugins.sharedmind.MapSharingController;

public class GetMapWindow {
    public GetMapWindow(JFrame frame, MapSharingController mpc) {
    	String[] options = { "Yes", "No" };
    	String question = "Do you have previous version of the map?";
    	
    	int result = JOptionPane.showOptionDialog(frame, question, "Get Map",
    			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
    			null, options, options[0]);
    	
    	if (result == 0) {
    		mpc.setHasMap();
    	}
    	mpc.requestMap();
    }
}
