package plugins;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import plugins.MapsDiff.ChangeList.Change;
import plugins.MapsDiff.ChangeList.ChangeType;

import freemind.modes.MindMapNode;
import freemind.modes.mindmapmode.MindMapMapModel;

public class MapsDiff {
	private static class MaybeDeletedNodes {
		public static class MaybeDeletedNode {
			public String id;
			public MindMapNode node;
			
			public MaybeDeletedNode(String id, MindMapNode node) {
				this.id = id;
				this.node = node;
			}
			
			public boolean equals(Object object) {
				if (!(object instanceof MaybeDeletedNode))
					return false;
				return this.id == ((MaybeDeletedNode)object).id;
			}
		}
		
		private Vector<MaybeDeletedNode> list;
		
		public MaybeDeletedNodes() {
			list = new Vector<MaybeDeletedNode>();
		}
		
		public void addEntry(String id, MindMapNode node) {
			list.add(new MaybeDeletedNode(id, node));
		}
		
		public boolean containsNodeWithId(String id) {
			return list.contains(new MaybeDeletedNode(id, null));
		}
		
		public MaybeDeletedNode getNodeWithId(String id) {
			for (MaybeDeletedNode entry : list) {
				if (entry.id.equals(id)) {
					return entry;
				}
			}
			return null;
		}
		
		public void removeEntry(String id) {
			list.remove(getNodeWithId(id));
		}
		
		public Vector<MaybeDeletedNode> getList() {
			return list;
		}
	}
	
	public static class ChangeList {
		public static enum ChangeType {
			EDITED, ADDED, DELETED, PARENT_CHANGED
		}
		
		public static class Change {
			public String id;
			public ChangeType type;
			public boolean conflicting;
			
			public Change(String id, ChangeType type) {
				this.id = id;
				this.type = type;
				this.conflicting = false;
			}
			
			public boolean equals(Object object) {
				if (!(object instanceof Change))
					return false;
				Change change = (Change)object;
				return change.id.equals(this.id) && change.type == this.type;
			}
			
			public String toString() {
				return id + " " + type + " " + conflicting;
			}
		}
		
		private Vector<Change> list;
		
		public ChangeList() {
			list = new Vector<Change>();
		}
		
		public Vector<Change> getList() {
			return list;
		}
		
		public boolean contains(Change change) {
			return list.contains(change);
		}
		
		public Change getEntry(String id, ChangeType type) {
			for (Change entry : list) {
				if (entry.equals(new Change(id, type))) {
					return entry;
				}
			}
			return null;
		}
	}
	
	private MapSharingController mpc;
	private MindMapMapModel base_map;
	private MindMapMapModel changed_map;
	private HashMap<String, MindMapNode> base_map_nodes;
	private HashMap<String, MindMapNode> changed_map_nodes;
	
	private ChangeList changes_list;
	private Vector<String> edited_nodes;
	private HashMap<String, String> added_nodes;
	private Vector<String> deleted_nodes;
	private HashMap<String, String> parent_changed_nodes;
	private MaybeDeletedNodes maybe_deleted_nodes;
	
	public MapsDiff(MapSharingController mpc, MindMapMapModel base_map, MindMapMapModel changed_map) {
		this.mpc = mpc;
		this.base_map = base_map;
		this.changed_map = changed_map;
		this.base_map_nodes = MapHelper.getNodeList(base_map.getRootNode());
		this.changed_map_nodes = MapHelper.getNodeList(changed_map.getRootNode());
		this.changes_list = new ChangeList();
		this.edited_nodes = new Vector<String>();
		this.added_nodes = new HashMap<String, String>();
		this.deleted_nodes = new Vector<String>();
		this.parent_changed_nodes = new HashMap<String, String>();
		this.maybe_deleted_nodes = new MaybeDeletedNodes();
		
		diffAttribute();
		diffStructure(base_map.getRootNode(), changed_map.getRootNode());
		handleMaybeDeleted();
	}

	private void diffAttribute() {
		for (Map.Entry<String, MindMapNode> entry : base_map_nodes.entrySet()) {
			if (changed_map_nodes.containsKey(entry.getKey()) &&
					!NodeComparator.nodeEqual(entry.getValue(), changed_map_nodes.get(entry.getKey()))) {
				edited_nodes.add(entry.getKey());
				changes_list.getList().add(new ChangeList.Change(entry.getKey(), ChangeList.ChangeType.EDITED));
			}
		}
	}
	
	private void diffStructure(MindMapNode base_node, MindMapNode changed_node) {
		HashMap<String, MindMapNode> base_node_children = MapHelper.getChildList(base_node);
		HashMap<String, MindMapNode> changed_node_children = MapHelper.getChildList(changed_node);
		
		for (Map.Entry <String, MindMapNode> child : base_node_children.entrySet()) {
			if (changed_node_children.containsKey(child.getKey())) {
				// node still exists in the same position in both tree
				diffStructure(child.getValue(), changed_node_children.get(child.getKey()));
			} else {
				// node doesn't exists in the same position in changed tree
				addSubtreeToMaybeDeleted(child.getValue());
			}
		}
		
		for (Map.Entry <String, MindMapNode> child : changed_node_children.entrySet()) {
			if (!base_node_children.containsKey(child.getKey())) {
				// node doesn't exists in the same position in base map
				addSubtreeToAddedNodes(child.getValue());
			}
		}
	}
	
	private void handleMaybeDeleted() {
		Vector<MaybeDeletedNodes.MaybeDeletedNode> maybe_deleted_list = maybe_deleted_nodes.getList();
		for (MaybeDeletedNodes.MaybeDeletedNode entry : maybe_deleted_list) {
			if (added_nodes.containsKey(entry.id)) {
				added_nodes.remove(entry.id);
				String old_parent_id = entry.node.getParentNode().getObjectId(
						entry.node.getMap().getModeController());
				String new_parent_id = changed_map_nodes.get(entry.id).getParentNode()
						.getObjectId(entry.node.getMap().getModeController());
				if (!old_parent_id.equals(new_parent_id)) {
					parent_changed_nodes.put(entry.id, 
							entry.node.getParentNode().getObjectId(entry.node.getMap().getModeController()));
					changes_list.getEntry(entry.id, ChangeType.ADDED).type= ChangeList.ChangeType.PARENT_CHANGED;
				} else {
					changes_list.getList().remove(changes_list.getEntry(entry.id, ChangeType.ADDED));
				}
			} else {
				deleted_nodes.add(entry.id);
				changes_list.getList().add(new ChangeList.Change(entry.id, ChangeList.ChangeType.DELETED));
			}
		}
	}
	
	private void addSubtreeToAddedNodes(MindMapNode node) {
		added_nodes.put(node.getObjectId(node.getMap().getModeController()), 
				node.getParentNode().getObjectId(node.getMap().getModeController()));
		changes_list.getList().add(new ChangeList.Change(
				node.getObjectId(node.getMap().getModeController()), ChangeList.ChangeType.ADDED));
		HashMap<String, MindMapNode> children = MapHelper.getChildList(node);
		for (MindMapNode child : children.values()) {
			addSubtreeToAddedNodes(child);
		}
	}
	
	private void addSubtreeToMaybeDeleted(MindMapNode node) {
		HashMap<String, MindMapNode> children = MapHelper.getChildList(node);
		for (MindMapNode child : children.values()) {
			addSubtreeToMaybeDeleted(child);
		}
		maybe_deleted_nodes.addEntry(node.getObjectId(node.getMap().getModeController()), node);
	}
	
	public ChangeList getChangesList() {
		return changes_list;
	}
	
	public Vector<String> getEditedNodes() {
		return edited_nodes;
	}
	
	public HashMap<String, String> getAddedNodes() {
		return added_nodes;
	}
	
	public Vector<String> getDeletedNodes() {
		return deleted_nodes;
	}
	
	public HashMap<String, String> getParentChangedNodes() {
		return parent_changed_nodes;
	}
	
	public boolean isSubtreeModified(String subtree_root_id) {
		return isSubtreeModified(changed_map_nodes.get(subtree_root_id));
	}
	
	public boolean isSubtreeModified(MindMapNode subtree_root) {
		Collection<String> node_list = MapHelper.getNodeList(subtree_root).keySet();
		Collection<String> added_nodes_parent_list = added_nodes.values();
		for (String id : node_list) {
			if (edited_nodes.contains(id) || added_nodes_parent_list.contains(id))
				return true;
		}
		return false;
	}
	
	public void markAllChangesToSubtreeConflicting(String subtree_root_id) {
		MindMapNode subtree_root = changed_map_nodes.get(subtree_root_id);
		Collection<String> node_list = MapHelper.getNodeList(subtree_root).keySet();
		for (String id : node_list) {
			if (edited_nodes.contains(id)) {
				changes_list.getEntry(id, ChangeType.EDITED).conflicting = true;
			}
			for (Map.Entry<String, String> entry: added_nodes.entrySet()) {
				if (entry.getValue().equals(id))
				 changes_list.getEntry(entry.getKey(), ChangeType.ADDED).conflicting = true;
			}
			for (Map.Entry<String, String> entry: parent_changed_nodes.entrySet()) {
				if (entry.getValue().equals(id)) {
					markAllChangesToSubtreeConflicting(entry.getKey());
				}
			}
		}
	}
}
