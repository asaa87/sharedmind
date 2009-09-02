package plugins.sharedmind;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MergingWindow extends JDialog{
	private MapSharingController mpc;
	private JPanel panel1;
	private JPanel panel2;
	private JPanel panel3;
	private JButton nextButton;
	private JButton prevButton;
	private JButton markResolvedButton;
	private JLabel progressLabel;
	
	public MergingWindow(Frame owner, MapSharingController mpc) {
		super(owner);
		this.mpc = mpc;
		
		initializeComponent();
		
		this.nextButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showNextConflict();
			}
			
		});
		
		this.prevButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showPreviousConflict();
			}
			
		});
		
		this.markResolvedButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				markAsResolved();
			}
			
		});
		
        final MergingWindow merging_window = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                showNextConflict();
                updateProgressLabel();
                merging_window.setVisible(true);
            }
        });
	}

	private void initializeComponent() {
		this.setTitle("Map Merging");
		
		this.panel1 = new JPanel();
		this.panel1.setLayout(new GridLayout(2, 1));
		this.panel2 = new JPanel();
		this.panel2.setLayout(new FlowLayout());
		this.panel3 = new JPanel(new FlowLayout());
		
		this.nextButton = new JButton("Next");
		this.prevButton = new JButton("Previous");
		this.markResolvedButton = new JButton("Mark as resolved");
		this.progressLabel = new JLabel();
		
		this.getContentPane().add(this.panel1);
		this.panel1.add(this.panel3);
		this.panel3.add(this.progressLabel);
		this.panel1.add(this.panel2);
		this.panel2.add(this.nextButton);
		this.panel2.add(this.prevButton);
		this.panel2.add(this.markResolvedButton);
		
		this.setSize(300, 100);
	}
	
	private void showNextConflict() {
		mpc.showConflict(true);
	}
	
	private void showPreviousConflict() {
		mpc.showConflict(false);
	}
	
	private void markAsResolved() {
		if (!mpc.markAsResolved()) {
			mpc.onMergeFinished();
			this.setVisible(false);
			this.dispose();
		} else {
			updateProgressLabel();
		}
	}
	
	private void updateProgressLabel() {
		this.progressLabel.setText(
				"Resolved " + mpc.getNumberOfResolvedConflict() 
				+ " out of " + mpc.getNumberOfConflict() + " conflict(s).");
	}
}
