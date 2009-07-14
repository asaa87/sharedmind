package plugins;

import freemind.modes.MindMapCloud;
import freemind.modes.MindMapEdge;
import freemind.modes.MindMapNode;

public class NodeComparator {
	/**
	 * Check whether text, link, color, edge, font, icon, cloud, background
	 * color, and attributes are the same
	 * 
	 * @param node
	 * @return
	 */
	public static boolean nodeEqual(MindMapNode node1, MindMapNode node2) {
		// check text
		if (!objectEquals(node1.getPlainTextContent(), node2
				.getPlainTextContent()))
			return false;
		// check link
		if (!objectEquals(node1.getLink(), node2.getLink()))
			return false;
		// check color
		if (!objectEquals(node1.getColor(), node2.getColor()))
			return false;
		// check edge
		MindMapEdge edge1 = node1.getEdge();
		MindMapEdge edge2 = node2.getEdge();
		if (edge1 != null && edge2 != null) {
			if (!objectEquals(edge1.getStyle(), edge2.getStyle())
					|| !objectEquals(edge1.getColor(), edge2.getColor())
					|| edge1.getWidth() != edge2.getWidth())
				return false;
		} else if (!(edge1 == null && edge2 == null)) {
			return false;
		}
		// check font
		if (!objectEquals(node1.getFont(), node2.getFont()))
			return false;
		// check icon
		if (!objectEquals(node1.getIcons(), node2.getIcons()))
			return false;
		// check cloud
		if (!cloudEquals(node1.getCloud(), node2.getCloud()))
			return false;
		// check background color
		if (!objectEquals(node1.getBackgroundColor(), node2
				.getBackgroundColor()))
			return false;
		// check attributes
		if (!objectEquals(node1.getAttributes().getAttributes(), node2
				.getAttributes().getAttributes()))
			return false;
		return true;
	}

	private static boolean objectEquals(Object s1, Object s2) {
		if (s1 != null && s2 != null)
			return s1.equals(s2);
		else if (s1 == null && s2 == null)
			return true;
		else
			return false;
	}
	
	private static boolean cloudEquals(MindMapCloud cloud1, MindMapCloud cloud2) {
		if (cloud1 == null && cloud2 == null)
			return true;
		if (cloud1 != null && cloud2!= null) {
			return cloud1.getColor().equals(cloud2.getColor())
					&& cloud1.getExteriorColor().equals(cloud2.getExteriorColor())
					&& cloud1.getRealWidth() == cloud2.getRealWidth()
					&& cloud1.getStyle().equals(cloud2.getStyle())
					&& cloud1.getWidth() == cloud2.getWidth();
		}
		return false;
	}
}
