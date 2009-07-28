package plugins;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class ConnectingWindow {
	MapSharingController mpc;
	JDialog window;
	
	
    public ConnectingWindow(JFrame frame, MapSharingController mpc) {
    	System.out.println("show connecting window");
    	this.mpc = mpc;
    	this.window = new JDialog(frame);
    	this.window.setModal(true);
    	this.window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    	this.window.setTitle("Connecting ...");
    	this.window.setSize(300, 100);
        final JDialog window = this.window;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                window.setVisible(true);
            }
        });
    }
    
    public void hide() {
    	System.out.println("hide connecting window");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	window.setVisible(false);
            	window.dispose();
            }
        });

    }
}