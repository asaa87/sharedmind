package plugins.sharedmind.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import plugins.sharedmind.MapSharingController;
import plugins.sharedmind.synchronouscollaboration.SharedAction;

import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.NodeAction;
import freemind.controller.actions.generated.instance.XmlAction;

import sun.awt.VariableGridLayout;

public class RedoConflictingActionsWindow {
	private static RedoConflictingActionsWindow instance;
	
	private MapSharingController mpc;
	private JDialog window;
	private JTable local_action_table;
	private JTable action_table;
	private JButton ok_button;
	private JButton check_all_button;
	private JPanel panel1;
	private JPanel panel3;
	private JLabel label1;
	private JLabel label2;
	private Vector<SharedAction> undoed_action;
	private Vector<SharedAction> undoed_local_action;
	
	private RedoConflictingActionsWindow(Frame owner, MapSharingController mpc) {
		this.mpc = mpc;
    	this.window = new JDialog(owner);
		initializeComponent();
		
		this.check_all_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkAll();
			}
			
		});
		
		this.ok_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				onSubmit();
			}
			
		});
	}

	private void initializeComponent() {
    	this.window.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    	this.window.setTitle("Redo Undoed Actions");
    	
		this.ok_button = new JButton("Redo Checked Actions");
		this.check_all_button = new JButton("Check All");
		
		this.panel1 = new JPanel();
		this.panel1.setLayout(new BoxLayout(this.panel1, BoxLayout.Y_AXIS));
		this.panel3 = new JPanel(new FlowLayout());
		this.panel3.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.label1 = new JLabel("Local undoed actions:");
		this.label1.setMinimumSize(new Dimension(350, 20));
		this.label1.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.label2 = new JLabel("All undoed actions:");
		this.label2.setMinimumSize(new Dimension(350, 20));
		this.label2.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.local_action_table = new JTable();
		this.local_action_table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		this.local_action_table.setPreferredScrollableViewportSize(new Dimension(350, 100));
		JScrollPane scrollPane1 = new JScrollPane(this.local_action_table);
		scrollPane1.setAlignmentX(Component.LEFT_ALIGNMENT);
        
		this.action_table = new JTable();
		this.action_table.setPreferredScrollableViewportSize(new Dimension(350, 200));
		JScrollPane scrollPane2 = new JScrollPane(this.action_table);
		scrollPane2.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.window.add(this.panel1);
		this.panel1.add(Box.createRigidArea(new Dimension(0,3)));
		this.panel1.add(label1);
		this.panel1.add(Box.createRigidArea(new Dimension(0,3)));
		this.panel1.add(scrollPane1);
		this.panel1.add(Box.createRigidArea(new Dimension(0,10)));
		this.panel1.add(label2);
		this.panel1.add(Box.createRigidArea(new Dimension(0,3)));
		this.panel1.add(scrollPane2);
		this.panel1.add(this.panel3);
		this.panel3.add(this.check_all_button);
		this.panel3.add(this.ok_button);
	}
	
	public static void showRedoConflictingActionsWindow(Frame owner, MapSharingController mpc) {
		if (instance == null)
			instance = new RedoConflictingActionsWindow(owner, mpc);
		
		instance.refreshContent();
		
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                instance.window.setVisible(true);
            }
        });
	}

	private void refreshContent() {
		this.undoed_action = mpc.getSynchronousEditingHistory().getUndoedChanges();
		this.undoed_local_action = new Vector<SharedAction>();
		for (SharedAction action : this.undoed_action) {
			if (action.getFrom().equals(mpc.getConnection().getUserName()))
				this.undoed_local_action.add(action);
		}

		initializeLocalActionTable();
		initializeActionTable();
		
		this.window.setSize(this.panel1.getLayout().preferredLayoutSize(this.panel1));
		this.window.setResizable(false);
	}
	
	private void initializeLocalActionTable() {
		this.local_action_table.setModel(new AbstractTableModel() {
			public boolean redo[] = new boolean[undoed_local_action.size()];
			public String column_title[] = { "Redo", "Action" };
			
			@Override
		    public String getColumnName(int columnIndex) {
				return column_title[columnIndex];
			}
			
			@Override
			public int getColumnCount() {
				return 2;
			}

			@Override
			public int getRowCount() {
				return undoed_local_action.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				switch (columnIndex) {
					case 0:
						return redo[rowIndex];
					case 1:
						XmlAction do_action = 
							undoed_local_action.get(rowIndex).getActionPair().getDoAction();
						return getActionDescription(do_action);
					default:
						return null;
				}
			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return columnIndex == 0;
			}
			
			@Override
		    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				if (columnIndex == 0 && aValue instanceof Boolean) {
					redo[rowIndex] = ((Boolean) aValue).booleanValue();
					fireTableCellUpdated(rowIndex, columnIndex);
				}
		    }
			
		});
		TableColumn redo_column = this.local_action_table.getColumnModel().getColumn(0);
		JCheckBox cell_editor = new JCheckBox();
		cell_editor.setAlignmentX(Component.CENTER_ALIGNMENT);
		redo_column.setCellEditor(new DefaultCellEditor(cell_editor));
		redo_column.setCellRenderer(
				new TableCellRenderer() {
					// the method gives the component  like whom the cell must be rendered
                    public Component getTableCellRendererComponent(
                    		JTable table, Object value, boolean isSelected,
							boolean isFocused, int row, int col) {
						boolean marked = (Boolean) value;
						JCheckBox rendererComponent = new JCheckBox();
						rendererComponent.setAlignmentX(Component.CENTER_ALIGNMENT);
						if (marked) {
							rendererComponent.setSelected(true);
						}
						return rendererComponent;
					}
				});
		this.local_action_table.getColumnModel().getColumn(0).setPreferredWidth(50);
		this.local_action_table.getColumnModel().getColumn(1).setPreferredWidth(300);
	}
	
	private void initializeActionTable() {
		this.action_table.setModel(new AbstractTableModel() {
			public String column_title[] = { "User", "Action" };
			
			@Override
		    public String getColumnName(int columnIndex) {
				return column_title[columnIndex];
			}
			
			@Override
			public int getColumnCount() {
				return 2;
			}

			@Override
			public int getRowCount() {
				return undoed_action.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				switch (columnIndex) {
					case 0:
						return undoed_action.get(rowIndex).getFrom();
					case 1:
						XmlAction do_action = 
							undoed_action.get(rowIndex).getActionPair().getDoAction();
						return getActionDescription(do_action);
					default:
						return null;
				}
			}
			
		});
		this.action_table.getColumnModel().getColumn(0).setPreferredWidth(50);
		this.action_table.getColumnModel().getColumn(1).setPreferredWidth(300);
	}

	private void checkAll() {
		TableModel model = this.local_action_table.getModel();
		for (int i = 0; i < model.getRowCount(); ++i) {
			model.setValueAt(true, i, 0);
		}
	}

	
	private void onSubmit() {
		TableModel model = this.local_action_table.getModel();
		boolean succeed = true;
		Vector<SharedAction> redone_succeeded = new Vector<SharedAction>();
		for (int i = 0; i < model.getRowCount(); ++i) {
			if ((Boolean)model.getValueAt(i, 0)) {
				boolean return_value = mpc.getController().getActionFactory().executeAction(
						((SharedAction) this.undoed_local_action.get(i)).getActionPair());
				if (return_value) {
					redone_succeeded.add(this.undoed_local_action.get(i));
				} else {
					succeed = false;
				}
			}
		}
		if (!succeed) {
			JOptionPane.showMessageDialog(this.window, 
					"Some of the actions you have choosen depends on other actions.", 
					"Failed Redo", JOptionPane.ERROR_MESSAGE);
			this.undoed_local_action.removeAll(redone_succeeded);
			this.undoed_action.removeAll(redone_succeeded);
			initializeLocalActionTable();
			initializeActionTable();
			this.local_action_table.repaint();
			this.action_table.repaint();
		} else {
			this.window.setVisible(false);
		}
	}
	
	private String getActionDescription(XmlAction action) {
		String class_name = " ";
		String node_id = "";
		String node_label = "";
		if (action instanceof NewNodeAction) {
			class_name = action.getClass().getSimpleName();
			node_id = ((NewNodeAction) action).getNewId();
		} else if (action instanceof NodeAction) {
			class_name = action.getClass().getSimpleName();
			node_id = ((NodeAction) action).getNode();
		} else if (action instanceof CompoundAction) {
			CompoundAction compoundAction = (CompoundAction) action;
			XmlAction last_action = (XmlAction) 
				compoundAction.getChoice(compoundAction.getListChoiceList().size() - 1);
			if (last_action instanceof NodeAction) {
				class_name = last_action.getClass().getSimpleName();
				node_id = ((NodeAction) last_action).getNode();
			}
		}
		try {
			node_label = mpc.getController().getNodeFromID(node_id).getPlainTextContent();
		} catch (IllegalArgumentException e) {
			// node is not in current map
		}
		return class_name + " " + node_id + " : '" + node_label + "'";
	}
}
