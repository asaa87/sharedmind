/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2006  Christian Foltin and others
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
/* $Id: NodeNoteViewer.java,v 1.1.2.1.2.12 2008/04/11 16:58:31 christianfoltin Exp $ */
package freemind.modes.browsemode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import freemind.modes.ControllerAdapter;
import freemind.modes.MindMapNode;
import freemind.modes.ModeController.NodeSelectionListener;
import freemind.modes.common.plugins.NodeNoteBase;
import freemind.view.mindmapview.NodeView;

/**
 * @author foltin
 * 
 */
public class NodeNoteViewer extends NodeNoteBase implements
		NodeSelectionListener {
	private JComponent noteScrollPane;

	private JLabel noteViewer;

	private final ControllerAdapter mBrowseController;
	
    private static ImageIcon noteIcon = null;

	public NodeNoteViewer(ControllerAdapter pBrowseController) {
		mBrowseController = pBrowseController;
	}

	protected JComponent getNoteViewerComponent(String text) {
		if (noteViewer == null) {
			noteViewer = new JLabel();
			noteViewer.setBackground(Color.WHITE);
			noteViewer.setVerticalAlignment(JLabel.TOP);
			noteViewer.setOpaque(true);
			noteScrollPane = new JScrollPane(noteViewer);
			noteScrollPane.setPreferredSize(new Dimension(1, 200));
		}
		noteViewer.setText(text != null ? text : "");
		return noteScrollPane;
	}

	public void onDeselectHook(NodeView pNode) {
		mBrowseController.getFrame().removeSplitPane();
	}

	public void onSelectHook(NodeView pNode) {
		String noteText = pNode.getModel().getNoteText();
		if(noteText != null && !noteText.equals("")){
//			 logger.info("Panel added");
			mBrowseController.getFrame().insertComponentIntoSplitPane(getNoteViewerComponent(noteText));
		}
	}

	public void onSaveNode(MindMapNode pNode) {
	}

	public void onUpdateNodeHook(MindMapNode pNode) {
		setStateIcon(pNode, true);
	}
	
	/** Copied from NodeNoteRegistration.*/
    protected void setStateIcon(MindMapNode node, boolean enabled) {
        // icon
        if (noteIcon == null) {
            noteIcon = new ImageIcon(mBrowseController
                    .getResource("images/knotes.png"));
        }
        node.setStateIcon(this.getClass().getName(), (enabled) ? noteIcon
                : null);
    }

}
