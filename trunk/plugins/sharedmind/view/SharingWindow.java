package plugins.sharedmind.view;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import plugins.sharedmind.MapSharingController;

/*
 * SharingWindow.java
 *
 * Created on October 27, 2008, 12:57 AM
 */

/**
 *
 * @author  asaa
 */
public class SharingWindow extends javax.swing.JFrame {
    private javax.swing.JButton createButton;
    private javax.swing.JButton subscribeButton;
    private javax.swing.JButton unsubscribeButton;
    private javax.swing.JButton sendButton;
    private javax.swing.JButton changeColorButton;
    private javax.swing.JButton gmomoButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTextPane chatPane;
    private javax.swing.JTextArea messageArea;
    private javax.swing.JTextArea onlineUserListArea;
    private javax.swing.JTextPane checkpointingPane;
    private javax.swing.JTextField topicField;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JCheckBox propagateFoldActionCheckbox;
    private ColorChooserDialog colorChooserDialog;
	private final MapSharingController mpc;
    
    public SharingWindow(final MapSharingController mpc) {
    	this.mpc = mpc;
        initComponents();
        this.setTitle("Sharing chat: user "+mpc.getNetworkUserId()+" port "+mpc.getNetworkPort());
        this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				mpc.stopCollaboration();
			}
        });
        createButton.addActionListener(new ActionListener() {
        	JTextField topicFieldcopy = topicField;
        	
			public void actionPerformed(ActionEvent arg0) {
				mpc.createTopic(topicFieldcopy.getText());
			}
        });
        subscribeButton.addActionListener(new ActionListener() {
        	JTextField topicFieldcopy = topicField;

			public void actionPerformed(ActionEvent e) {
				mpc.subscribeToTopic(topicFieldcopy.getText());
			}
        });
        unsubscribeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mpc.unsubscribeToTopic();
			}
        	
        });
        gmomoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				onInvite();
			}
        	
        });
        changeColorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showColorChooserDialog();
			}
        	
        });
        sendButton.addActionListener(new ActionListener() {
        	final javax.swing.JTextArea messageAreaCopy = messageArea;

			public void actionPerformed(ActionEvent e) {
				mpc.sendChat(messageAreaCopy.getText());
				messageAreaCopy.setText("");
			}
        	
        });
        
        messageArea.addKeyListener(new KeyListener() {
            final javax.swing.JTextArea messageAreaCopy = messageArea;

                    @Override
                    public void keyPressed(KeyEvent e) {
                            // TODO Auto-generated method stub
                            
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                                    mpc.sendChat(messageAreaCopy.getText().trim());
                                    messageAreaCopy.setText("");
                            }
                    }

                    @Override
                    public void keyTyped(KeyEvent e) {
                            // TODO Auto-generated method stub
                            
                    }
            
        });

        propagateFoldActionCheckbox.addItemListener(new ItemListener() {
		
			@Override
			public void itemStateChanged(ItemEvent e) {
				mpc.setPropagateFoldAction(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
        final SharingWindow sharing_window = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sharing_window.setVisible(true);
            }
        });
    }
    
    protected void showColorChooserDialog() {
		if (this.colorChooserDialog == null) {
			this.colorChooserDialog = new ColorChooserDialog(this, this.mpc);
		}
		this.colorChooserDialog.showDialog();
	}

	public void addChat(String chat, Color color) {
    	StyledDocument doc = chatPane.getStyledDocument();
    	Style def = StyleContext.getDefaultStyleContext().getStyle( StyleContext.DEFAULT_STYLE );
    	Style colored = doc.addStyle(null, def);
    	StyleConstants.setForeground(colored, color);
    	try {
			doc.insertString(doc.getLength(), chat + "\n", colored);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }
    
    private void addCheckpointStatus(String status) {
    	StyledDocument doc = checkpointingPane.getStyledDocument();
    	Style def = StyleContext.getDefaultStyleContext().getStyle( StyleContext.DEFAULT_STYLE );
    	Style regular = doc.addStyle("regular", def);
    	try {
			doc.insertString(doc.getLength(), status + "\n", regular);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		checkpointingPane.setCaretPosition(checkpointingPane.getDocument().getLength());
    }
    
    public void setOnlineUserList(Vector<String> user_list) {
    	onlineUserListArea.setText("");
    	for (String user : user_list) {
    		onlineUserListArea.append(user + "\n");
    	}
    }
    
    public void setVersion(int version) {
    	statusLabel.setText("Map Version: " + version);
    	statusLabel.repaint();
    }
    
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        topicField = new javax.swing.JTextField();
        createButton = new javax.swing.JButton();
        subscribeButton = new javax.swing.JButton();
        unsubscribeButton = new javax.swing.JButton();
        changeColorButton = new javax.swing.JButton();
        gmomoButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatPane = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        messageArea = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        onlineUserListArea = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        checkpointingPane = new javax.swing.JTextPane();
        sendButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        propagateFoldActionCheckbox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        createButton.setText("Create Topic");
        subscribeButton.setText("Connect");
        unsubscribeButton.setText("Disconnect");
        changeColorButton.setText("Change Color");
        gmomoButton.setText("Google Chat Login");

        chatPane.setSize(200, 75);
        chatPane.setEditable(false);
        jScrollPane1.setViewportView(chatPane);

        messageArea.setColumns(20);
        messageArea.setRows(5);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(messageArea);

        sendButton.setText("Send");
        
        onlineUserListArea.setColumns(15);
        onlineUserListArea.setEditable(false);
        onlineUserListArea.setRows(5);
        jScrollPane3.setViewportView(onlineUserListArea);
        
        checkpointingPane.setSize(200, 75);
        checkpointingPane.setEditable(false);
        jScrollPane4.setViewportView(checkpointingPane);
        
        tabbedPane.addTab("Chat", jScrollPane1);
        tabbedPane.addTab("Checkpointing", jScrollPane4);
        
        statusLabel.setText("");
        
        propagateFoldActionCheckbox.setText("Share folding of node");
        propagateFoldActionCheckbox.setSelected(false);
        propagateFoldActionEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                	.addGroup(jPanel1Layout.createSequentialGroup()
                		.addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 470, Short.MAX_VALUE)
                		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                		.addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(topicField, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
//                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
//                        .addComponent(createButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(subscribeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unsubscribeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gmomoButton))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(propagateFoldActionCheckbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(changeColorButton))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 470, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(topicField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
//                    .addComponent(createButton)
                    .addComponent(subscribeButton)
                    .addComponent(unsubscribeButton)
                    .addComponent(gmomoButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                		.addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                		.addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE))  
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                		.addComponent(statusLabel)
                		.addComponent(propagateFoldActionCheckbox)
                		.addComponent(changeColorButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    public void dispose() {
    	super.dispose();
    	mpc.exitSharing();
    }

	public void propagateFoldActionEnabled(boolean b) {
		this.propagateFoldActionCheckbox.setEnabled(b);
	}

	public void startCheckpointing() {
		addCheckpointStatus("----- Start checkpointing -----");
	}

	public void stopCheckpointing() {
		addCheckpointStatus("----- Stop checkpointing -----");
	}
	
	public void onInvite() {
		if (!mpc.isGmomoAuthenticated()) {
			mpc.showGmomoLoginWindow();
		} else {
			this.gmomoButton.setText("Google Chat Invitation");
			mpc.showGmomoContactListWindow();
		}
	}
}

