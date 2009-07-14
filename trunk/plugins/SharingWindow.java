package plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JTextField;

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
    private javax.swing.JButton mergeFinishedButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea chatArea;
    private javax.swing.JTextArea messageArea;
    private javax.swing.JTextField topicField;
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
        sendButton.addActionListener(new ActionListener() {
        	final javax.swing.JTextArea messageAreaCopy = messageArea;

			public void actionPerformed(ActionEvent e) {
				mpc.sendChat(messageAreaCopy.getText());
				messageAreaCopy.setText("");
			}
        	
        });
        mergeFinishedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnableMergeFinishedButton(false);
				mpc.onMergeFinished();
			}
        	
        });
        final SharingWindow sharing_window = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sharing_window.setVisible(true);
            }
        });
    }
    
    public void setEnableMergeFinishedButton(boolean enabled) {
    	mergeFinishedButton.setEnabled(enabled);
    	this.repaint();
    }
    
    public void addChat(String chat) {
    	chatArea.append(chat + "\n");
    }
    
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        topicField = new javax.swing.JTextField();
        createButton = new javax.swing.JButton();
        subscribeButton = new javax.swing.JButton();
        unsubscribeButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatArea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        messageArea = new javax.swing.JTextArea();
        sendButton = new javax.swing.JButton();
        mergeFinishedButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        createButton.setText("Create Topic");
        subscribeButton.setText("Subscribe to Topic");
        unsubscribeButton.setText("Unsubscribe");
        mergeFinishedButton.setText("Merge Finished");
        mergeFinishedButton.setEnabled(false);

        chatArea.setColumns(35);
        chatArea.setEditable(false);
        chatArea.setRows(5);
        jScrollPane1.setViewportView(chatArea);

        messageArea.setColumns(20);
        messageArea.setRows(5);
        jScrollPane2.setViewportView(messageArea);

        sendButton.setText("Send");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(topicField, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(createButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(subscribeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(unsubscribeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(mergeFinishedButton))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(createButton)
                    .addComponent(subscribeButton)
                    .addComponent(unsubscribeButton)
                    .addComponent(mergeFinishedButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE))
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
}

