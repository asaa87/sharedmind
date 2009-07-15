package plugins;

import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import plugins.MapsDiff.ChangeList.ChangeType;
import freemind.modes.MindMapNode;

public class ConflictList {
	public static enum ConflictType {
		DIFFERENT_ATTRIBUTES,
		NODE_DELETED_SUBTREE_MODIFIED,
		PARENTS_CHANGE,
		PARENT_CHANGES_NODE_DELETED,
		CYCLIC_PARENT
	};
	
	public class Conflict {
		public ConflictType type;
		public String id_v1;
		public String id_v2;
		public String common_root;
		
		public Conflict(ConflictType type, String id_v1, String id_v2) {
			this.type = type;
			this.id_v1 = id_v1;
			this.id_v2 = id_v2;
		}
	}
	
	private Vector<Conflict> conflict_list;
	
	public ConflictList(MapsDiff base_v1_diff, MapsDiff base_v2_diff,
			HashMap<String, MindMapNode> v1_nodes,
			HashMap<String, MindMapNode> v2_nodes) {
		this.conflict_list = new Vector<Conflict>();

		HashMap<String, String> v1_added = base_v1_diff.getAddedNodes();
		HashMap<String, String> v2_added = base_v2_diff.getAddedNodes();
		for (String id : v1_added.keySet()) {
			if (v2_added.containsKey(id)) {
				if (!NodeComparator.nodeEqual(v1_nodes.get(id), v2_nodes.get(id))) {
					addConflict(id, null, ConflictType.DIFFERENT_ATTRIBUTES);
					base_v1_diff.getChangesList().getEntry(id, ChangeType.ADDED).conflicting = true;
					base_v2_diff.getChangesList().getEntry(id, ChangeType.ADDED).conflicting = true;
				}
				if (!v1_added.get(id).equals(v2_added.get(id))) {
					addConflict(id, null, ConflictType.PARENTS_CHANGE);
					base_v1_diff.getChangesList().getEntry(id, ChangeType.ADDED).conflicting = true;
					base_v2_diff.getChangesList().getEntry(id, ChangeType.ADDED).conflicting = true;
				}
			}
		}
		
		Vector<String> v1_edited = base_v1_diff.getEditedNodes();
		Vector<String> v2_edited = base_v2_diff.getEditedNodes();
		for (String id : v1_edited) {
			if (v2_edited.contains(id) && !NodeComparator.nodeEqual(v1_nodes.get(id), v2_nodes.get(id))) {
				addConflict(id, null, ConflictType.DIFFERENT_ATTRIBUTES);
				base_v1_diff.getChangesList().getEntry(id, ChangeType.EDITED).conflicting = true;
				base_v2_diff.getChangesList().getEntry(id, ChangeType.EDITED).conflicting = true;
			}
		}
		
		Vector<String> v1_deleted = base_v1_diff.getDeletedNodes();
		for (String id : v1_deleted) {
			if (base_v2_diff.isSubtreeModified(id)) {
				addConflict(id, null, ConflictType.NODE_DELETED_SUBTREE_MODIFIED);
				base_v1_diff.getChangesList().getEntry(id, ChangeType.DELETED).conflicting = true;
				base_v2_diff.markAllChangesToSubtreeConflicting(id);
			}
		}
		Vector<String> v2_deleted = base_v2_diff.getDeletedNodes();
		for (String id : v2_deleted) {
			if (base_v1_diff.isSubtreeModified(id)) {
				addConflict(null, id, ConflictType.NODE_DELETED_SUBTREE_MODIFIED);
				base_v2_diff.getChangesList().getEntry(id, ChangeType.DELETED).conflicting = true;
				base_v1_diff.markAllChangesToSubtreeConflicting(id);
			}
		}
		
		HashMap<String, String> v1_parent_changed = base_v1_diff.getParentChangedNodes();
		HashMap<String, String> v2_parent_changed = base_v2_diff.getParentChangedNodes();
		for (Entry<String, String> entry : v1_parent_changed.entrySet()) {
			if (v2_parent_changed.containsKey(entry.getKey()) && 
					!entry.getKey().equals(v2_parent_changed.get(entry.getKey()))) {
				addConflict(entry.getKey(), null, ConflictType.PARENTS_CHANGE);
				base_v1_diff.getChangesList().getEntry(entry.getKey(), 
						ChangeType.PARENT_CHANGED).conflicting = true;
				base_v2_diff.getChangesList().getEntry(entry.getKey(), 
						ChangeType.PARENT_CHANGED).conflicting = true;
			}
		}
		
		for (String id : v1_deleted) {
			if (v2_parent_changed.containsKey(id)) {
				addConflict(id, null, ConflictType.PARENT_CHANGES_NODE_DELETED);
				base_v1_diff.getChangesList().getEntry(id, ChangeType.DELETED).conflicting = true;
				base_v2_diff.getChangesList().getEntry(id, ChangeType.PARENT_CHANGED).conflicting = true;
			}
		}
		for (String id : v2_deleted) {
			if (v1_parent_changed.containsKey(id)) {
				addConflict(null, id, ConflictType.PARENT_CHANGES_NODE_DELETED);
				base_v2_diff.getChangesList().getEntry(id, ChangeType.DELETED).conflicting = true;
				base_v1_diff.getChangesList().getEntry(id, ChangeType.PARENT_CHANGED).conflicting = true;
			}
		}
		
		for (String id1 : v1_parent_changed.keySet()) {
			if (!v2_parent_changed.containsKey(id1)) {
				for (String id2: v2_parent_changed.keySet()) {
					if (!v1_parent_changed.containsKey(id2) &&
							MapHelper.isDescendant(v1_nodes.get(id2), v1_nodes.get(id1)) &&
							MapHelper.isDescendant(v2_nodes.get(id1), v2_nodes.get(id2))) {
						addConflict(id1, id2, ConflictType.CYCLIC_PARENT);
						base_v1_diff.getChangesList().getEntry(id1, ChangeType.PARENT_CHANGED).conflicting = true;
						base_v2_diff.getChangesList().getEntry(id2, ChangeType.PARENT_CHANGED).conflicting = true;
					}
				}
			}
		}
	}
	
	private void addConflict(String node_id1, String node_id2, ConflictType type) {
		conflict_list.add(new Conflict(type, node_id1, node_id2));
	}
	
	public Vector<plugins.ConflictList.Conflict> getList() {
		return conflict_list;
	}
}
