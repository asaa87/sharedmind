package plugins;

import java.util.Map;

import javax.swing.ImageIcon;

import freemind.modes.mindmapmode.MindMapNodeModel;

public class NodeHelper {

	public static void copyNodeAttributes(MindMapNodeModel originalNode, MindMapNodeModel targetNode) {
		targetNode.setAdditionalInfo(originalNode.getAdditionalInfo());
		int attribute_num = originalNode.getAttributeTableLength();
		for (int i = 0; i < attribute_num; ++i) {
			targetNode.setAttribute(i, originalNode.getAttribute(0));
		}
		targetNode.setBackgroundColor(originalNode.getBackgroundColor());
		targetNode.setCloud(originalNode.getCloud());
		targetNode.setColor(originalNode.getColor());
		targetNode.setEdge(originalNode.getEdge());
		targetNode.setFolded(originalNode.isFolded());
		targetNode.setFont(originalNode.getFont());
		targetNode.setHGap(originalNode.getHGap());
		targetNode.setHistoryInformation(originalNode.getHistoryInformation());
		targetNode.setLeft(originalNode.isLeft());
		targetNode.setLink(originalNode.getLink());
		targetNode.setNoteText(originalNode.getNoteText());
		targetNode.setShiftY(originalNode.getShiftY());
		Map<String, ImageIcon> stateIcons = originalNode.getStateIcons();
		for (Map.Entry<String, ImageIcon> entry : stateIcons.entrySet()) {
			targetNode.setStateIcon(entry.getKey(), entry.getValue());
		}
		targetNode.setStyle(originalNode.getStyle());
		targetNode.setText(originalNode.getText());
		Map<String,String> toolTip = originalNode.getToolTip();
		for (Map.Entry<String, String> entry : toolTip.entrySet()) {
			targetNode.setToolTip(entry.getKey(), entry.getValue());
		}
		targetNode.setUnderlined(originalNode.isUnderlined());
		targetNode.setVGap(originalNode.getVGap());
		targetNode.setXmlNoteText(originalNode.getXmlNoteText());
	}
}
