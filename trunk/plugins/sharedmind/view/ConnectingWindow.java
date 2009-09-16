package plugins.sharedmind.view;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import plugins.sharedmind.MapSharingController;

public class ConnectingWindow {
	private MapSharingController mpc;
	private JDialog window;
	private JButton abortButton;
	private JPanel panel;
	private JPanel panel2;
	private JPanel panel3;
	private JLabel label;
	
    public ConnectingWindow(JFrame frame, MapSharingController mpc) {
    	System.out.println("show connecting window");
    	this.mpc = mpc;
    	this.window = new JDialog(frame);
    	this.window.setModal(true);
    	this.window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    	this.window.setTitle("Connecting ...");
    	this.window.setSize(300, 100);
    	
    	this.abortButton = new JButton("Abort");
		final MapSharingController mpc_copy = mpc;
    	this.abortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mpc_copy.unsubscribeToTopic();
				mpc_copy.hideConnectingWindow();
			}
    		
    	});
    	
    	this.label = new JLabel("Connecting ... Please wait ...");
    	
    	this.panel2 = new JPanel();
    	this.panel2.add(this.label);
    	this.panel3 = new JPanel();
    	this.panel3.add(this.abortButton);
    	
    	this.panel = new JPanel();
    	this.panel.setLayout(new GridLayout(2, 1));
    	this.panel.add(this.panel2);
    	this.panel.add(this.panel3);
    	this.window.add(this.panel);
    	
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