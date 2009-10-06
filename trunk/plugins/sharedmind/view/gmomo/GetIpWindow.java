package plugins.sharedmind.view.gmomo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import plugins.sharedmind.MapSharingController;

import momo.app.config.ResourceConfig;

public class GetIpWindow {
	private MapSharingController mpc;
	private ContactListWindow contact_list_window;
	
	private JDialog dialog;
	private JLabel ip_label;
	private JLabel port_label;
	private JTextField port_field;
	private JComboBox ip_combobox;
	private JButton ok_button;
	private JPanel panel1;
	private JPanel panel2;
	private JPanel panel3;
	
	private String ip;
	private int port;
	
	public GetIpWindow(JDialog dialog2,
			MapSharingController mpc,
			ContactListWindow contact_list_window) {
		this.mpc = mpc;
		this.contact_list_window = contact_list_window;
		
		this.dialog = new JDialog(dialog2);
		initializeComponent();
		this.ok_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ip = (String) ip_combobox.getSelectedItem().toString();
				try {
					port = Integer.parseInt(port_field.getText());
					dialog.setVisible(false);
					GetIpWindow.this.contact_list_window.inviteAll(ip, port);
				} catch (NumberFormatException exception) {
					port_field.setText("");
				}
			}
			
		});
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				dialog.setVisible(true);
			}
			
		});
	}

	private void initializeComponent() {
		this.dialog.setTitle("Your ip address and port");
		this.ip_label = new JLabel();
		this.port_label = new JLabel();
		this.ip_combobox = new JComboBox();
		this.port_field = new JTextField();
		this.ok_button = new JButton();
		this.panel1 = new JPanel();
		this.panel2 = new JPanel();
		this.panel3 = new JPanel();
		
		this.dialog.setModal(true);
		
		this.ip_label.setText("IP address:");
		this.port_label.setText("Port:");
		
		List<InetAddress> ip_addresses = ResourceConfig.getIPAddress();
		this.ip_combobox.setEditable(true);
		for (InetAddress ip_address : ip_addresses) {
			this.ip_combobox.addItem(ip_address.getHostAddress());
		}
		
		this.port_field.setEditable(true);
		this.port_field.setText(mpc.getConnection().getPort() + "");
		
		this.ok_button.setText("Ok");
		this.ok_button.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.ok_button.setMinimumSize(new Dimension(0, 20));
		
		this.panel1.setLayout(new BoxLayout(this.panel1, BoxLayout.Y_AXIS));
		this.panel2.setLayout(new BoxLayout(this.panel2, BoxLayout.X_AXIS));
		this.panel3.setLayout(new BoxLayout(this.panel3, BoxLayout.X_AXIS));
		
		this.dialog.add(this.panel1);
		this.panel1.add(this.panel2);
		this.panel1.add(this.panel3);
		this.panel1.add(this.ok_button);
		this.panel2.add(this.ip_label);
		this.panel2.add(this.ip_combobox);
		this.panel3.add(this.port_label);
		this.panel3.add(this.port_field);
		
		this.dialog.setSize(new Dimension(200, 100));
		this.dialog.setMinimumSize(new Dimension(200, 100));
		this.dialog.setResizable(false);
	}
}
