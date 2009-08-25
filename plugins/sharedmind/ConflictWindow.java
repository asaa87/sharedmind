package plugins.sharedmind;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ConflictWindow {
	private static ConflictWindow instance;
	
	private JPanel panel1;
	private JPanel panel2;
	private JPanel panel3;
	private JLabel label;
	private JDialog window;
	private JButton ok_button;
	
    private ConflictWindow(final JFrame frame) {
    	this.panel1 = new JPanel();
    	this.panel2 = new JPanel();
    	this.panel3 = new JPanel();
    	
    	this.label = new JLabel();
    	this.label.setText("There're conflicting changes!!");
    	
    	this.ok_button = new JButton();
    	this.ok_button.setText("OK");
    	
    	this.window = new JDialog(frame);
    	this.window.setModal(true);
    	this.window.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    	this.window.setTitle("Conflicting change");
    	this.window.setSize(300, 100);
        panel1.setLayout(new GridLayout(2,1));
        this.panel1.add(this.panel2);
        this.panel1.add(this.panel3);
    	this.panel2.add(this.label);
    	this.panel3.add(this.ok_button);
    	this.window.add(panel1);
    	
    	this.ok_button.addActionListener(new ActionListener() {
    		final JDialog window_copy = window;
    		
			@Override
			public void actionPerformed(ActionEvent e) {
				window_copy.setVisible(false);
			}
    		
    	});
	}
    
    public static void ShowConflictWindow(JFrame frame) {
    	if (instance == null)
    		instance = new ConflictWindow(frame);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                instance.window.setVisible(true);
            }
        });
    };
}
