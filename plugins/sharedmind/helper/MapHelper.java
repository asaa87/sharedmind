package plugins.sharedmind.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import freemind.controller.filter.util.SortedComboBoxModel;
import freemind.controller.filter.util.SortedMapVector;
import freemind.modes.MindMapNode;
import freemind.modes.ModeController;
import freemind.modes.attributes.Attribute;
import freemind.modes.attributes.AttributeRegistryElement;
import freemind.modes.mindmapmode.MindMapController;

public class MapHelper {
	public static HashMap<String,MindMapNode> getNodeList(MindMapNode root_node) {
		HashMap<String,MindMapNode> node_list = new HashMap<String,MindMapNode>();
		node_list.put(root_node.getObjectId(root_node.getMap().getModeController()), root_node);
		if (!root_node.isLeaf()) {
			List<MindMapNode> children = root_node.getChildren();
			for (MindMapNode mindMapNode : children) {
				node_list.putAll(getNodeList(mindMapNode));
			}
		}
		return node_list;
	}
	
	public static HashMap<String, MindMapNode> getChildList(MindMapNode parent) {
		List<MindMapNode> child_list = parent.getChildren();
		HashMap<String, MindMapNode> return_value = new HashMap<String, MindMapNode>();
		for (MindMapNode node : child_list) {
			return_value.put(node.getObjectId(parent.getMap().getModeController()), node);
		}
		return return_value;
	}
	
	public static boolean isDescendant(MindMapNode parent, MindMapNode descendant) {
		if (parent.getChildPosition(descendant) > -1)
			return true;
		if (!parent.isLeaf()) {
			boolean return_value = false;
			Collection<MindMapNode> children = getChildList(parent).values();
			for (MindMapNode child : children) {
				return_value = return_value || isDescendant(child, descendant);
			}
			return return_value;
		} else {
			return false;
		}
	}
	
	public static void copyAttributeRegistry(MindMapController source, MindMapController target) {
		SortedMapVector attributes = source.getModel().getRegistry().getAttributes().getElements();
		int attribute_number = attributes.size();
		for (int i = 0; i < attribute_number; ++i) {
			AttributeRegistryElement attr = (AttributeRegistryElement) attributes.getValue(i);
			target.getModel().getRegistry().getAttributes().registry((String)attr.getKey());
			SortedComboBoxModel values = attr.getValues();
			int value_number = values.getSize();
			for (int j = 0; j < value_number; ++j) {
				target.getModel().getRegistry().getAttributes().getElement(attr.getKey())
						.addValue((String)values.getElementAt(j));
			}
			target.getModel().getRegistry().getAttributes().getElement(attr.getKey())
					.setRestriction(attr.isRestricted());
			target.getModel().getRegistry().getAttributes().getElement(attr.getKey())
					.setVisibility(attr.isVisible());
		}
	}
}
