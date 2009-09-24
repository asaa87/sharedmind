package plugins.sharedmind.view;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import plugins.sharedmind.MapSharingController;

public class ColorChooserDialog {
	private JDialog window;
	private JPanel panel1;
	private JPanel panel2;
	private JColorChooser color_chooser;
	private JButton ok_button;
	private JButton cancel_button;
	
	private MapSharingController mpc;
	
	public ColorChooserDialog(JFrame parent, MapSharingController mpc) {
		this.window = new JDialog(parent);
		this.window.setModal(true);
		this.mpc = mpc;
		initComponent();
		this.ok_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				changeColor();
				java.awt.EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						hideDialog();
					}
					
				});
			}
			
		});
		
		this.cancel_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				java.awt.EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						hideDialog();
					}
					
				});
			}
			
		});
	}
	
	protected void changeColor() {
		this.mpc.setChatColor(this.color_chooser.getColor());
	}

	protected void hideDialog() {
		this.window.setVisible(false);
	}
	
	public void showDialog() {
		this.window.setVisible(true);
	}
	
	private void initComponent() {
		this.color_chooser = new JColorChooser();
		this.ok_button = new JButton();
		this.cancel_button = new JButton();
		this.panel1 = new JPanel();
		this.panel2 = new JPanel();
		
		this.ok_button.setText("Ok");
		this.cancel_button.setText("Cancel");
		

		this.panel1.setLayout(new BoxLayout(this.panel1, BoxLayout.Y_AXIS));
		
		this.panel2 = new JPanel(new FlowLayout());
		this.panel2.add(this.ok_button);
		this.panel2.add(this.cancel_button);
		
		this.color_chooser.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.panel2.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		this.panel1.add(this.color_chooser);
		this.panel1.add(this.panel2);
		
		this.window.setTitle("Choose chat text color");
		this.window.add(this.panel1);
		this.window.setSize(450, 425);
		this.window.setResizable(false);
		this.window.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}
}
