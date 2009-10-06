package plugins.sharedmind.view.gmomo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import plugins.sharedmind.MapSharingController;

public class ContactListWindow {
	private MapSharingController mpc;
	private JDialog contact_list_dialog;
	private JTree contact_tree;
	private JScrollPane tree_scrollpane;
	private JButton invite_button;
	private JPanel panel;
	
	public ContactListWindow(MapSharingController mpc, JFrame owner) {
		this.contact_list_dialog = new JDialog(owner);
		this.mpc = mpc;
		initializeComponents();
		
		this.invite_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showGetIpWindow();
			}
			
		});
	}

	private void initializeComponents() {
		this.contact_list_dialog.setTitle("Googletalk Contacts");
		this.contact_tree = new JTree();
		this.tree_scrollpane = new JScrollPane();
		this.invite_button = new JButton();
		this.panel = new JPanel();
		
		this.contact_tree.putClientProperty("JTree.lineStyle", "None");
		this.tree_scrollpane.setPreferredSize(new Dimension(200, 300));
		this.tree_scrollpane.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.tree_scrollpane.setViewportView(this.contact_tree);
		
		DefaultTreeCellRenderer cell_renderer = new DefaultTreeCellRenderer();
		this.contact_tree.setCellRenderer(cell_renderer);
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Contacts");
		this.contact_tree.setModel(new DefaultTreeModel(root));
		this.contact_tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		
		this.invite_button.setText("Invite");
		this.invite_button.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.invite_button.setMinimumSize(new Dimension(0, 20));
		
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));
		this.panel.add(this.tree_scrollpane);
		this.panel.add(this.invite_button);
		this.contact_list_dialog.add(this.panel);
		this.contact_list_dialog.setPreferredSize(new Dimension(200, 330));
		this.contact_list_dialog.setMinimumSize(new Dimension(200, 330));
		this.contact_list_dialog.setMaximumSize(new Dimension(200, 330));
		
	}
	
	public synchronized void removeContact(String xmpp_bare_address) {
		DefaultMutableTreeNode root = 
			(DefaultMutableTreeNode)this.contact_tree.getModel().getRoot();
		for (int i = 0; i < root.getChildCount(); ++i) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
			if (node.toString().equals(xmpp_bare_address)) {
				((DefaultTreeModel) this.contact_tree.getModel()).removeNodeFromParent(
						new DefaultMutableTreeNode(node));
				break;
			}
		}
	}
	
	public synchronized void addContact(String xmpp_bare_address) {
		DefaultMutableTreeNode root = 
			(DefaultMutableTreeNode)this.contact_tree.getModel().getRoot();
		((DefaultTreeModel) this.contact_tree.getModel()).insertNodeInto(
				new DefaultMutableTreeNode(xmpp_bare_address),
				root, root.getChildCount());
		if ( !this.contact_tree.isExpanded( 0 ) ) {
			this.contact_tree.expandRow( 0 );
		}
		
	}

	public void show() {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				contact_list_dialog.setVisible(true);
			}
			
		});
	}
	
	public void hide() {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				contact_list_dialog.setVisible(false);
			}
			
		});
	}
	
	protected void showGetIpWindow() {
		GetIpWindow ip_window = new GetIpWindow(this.contact_list_dialog,
				this.mpc, this);
	}

	protected void inviteAll(String ip, int port) {
		TreePath[] selection_paths = this.contact_tree.getSelectionPaths();
		for (TreePath selection_path: selection_paths) {
			DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode) selection_path.getLastPathComponent();
			mpc.invite(node.toString(), ip, port);
			
		}
	}
}
