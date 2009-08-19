package plugins.sharedmind;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ConflictWindow {
    public ConflictWindow(final JFrame frame) {
    	java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
		        JOptionPane.showMessageDialog(frame,
                        "There's a conflicting change.", 
                        "Conflict detected", JOptionPane.WARNING_MESSAGE);
			}
    		
    	});

    }
}
