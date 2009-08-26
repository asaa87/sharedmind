package plugins.sharedmind;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import plugins.sharedmind.ConflictList.Conflict;
import plugins.sharedmind.ConflictList.ConflictType;
import plugins.sharedmind.MapsDiff.ChangeList.Change;
import plugins.sharedmind.MapsDiff.ChangeList.ChangeType;

import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.main.XMLParseException;
import freemind.modes.MindIcon;
import freemind.modes.MindMapNode;
import freemind.modes.mindmapmode.MergedMapInterface;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.MindMapMapModel;
import freemind.modes.mindmapmode.MindMapNodeModel;

public class MergedMap implements MergedMapInterface {
	private Logger log = Logger.getLogger(MergedMap.class);
	
	private enum MapType { V1, V2, MERGED };
	
	private MapSharingController mpc;
	private ConflictList conflict_list;
	
	private MindMapController base_map;
	private MindMapController v1_map;
	private MindMapController v2_map;
	private MindMapController merged_map;
	
	private MindMapNode v1_root_node;
	private MindMapNode v2_root_node;
	private MindMapNode merged_root_node;
	
	private HashMap<String, String> merged_map_id_to_real_id;
	private HashMap<String, String> v1_real_id_index;
	private HashMap<String, String> v2_real_id_index;
	private HashMap<String, String> merged_real_id_index;
	
	private MapsDiff base_v1_diff;
	private MapsDiff base_v2_diff;
	
	private HashMap<String, MindMapNode> v1_nodes;
	private HashMap<String, MindMapNode> v2_nodes;
	
	private HashMap<String, Boolean> node_position_list;
	private int conflict_shown;
	
	// used for displaying conflict
	private Vector<MindMapNode> conflicting_nodes;
	
	/**
	 * merged map checkpoint will follow v2's checkpoint
	 * @param mpc
	 * @param base_map
	 * @param v1_map
	 * @param v2_map
	 */
	public MergedMap(MapSharingController mpc, MindMapController base_map,
			MindMapController v1_map, MindMapController v2_map) {
		StringWriter writer = new StringWriter();
		try {
			base_map.getModel().getXml(writer);
			log.debug("base_map ----- " + writer.toString());
			writer = new StringWriter();
			v1_map.getModel().getXml(writer);
			log.debug("mine ----- " + writer.toString());
			writer = new StringWriter();
			v2_map.getModel().getXml(writer);
			log.debug("theirs ----- " + writer.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.mpc = mpc;
		this.base_map = base_map;
		this.v1_map = v1_map;
		this.v2_map = v2_map;
		
		this.base_v1_diff = new MapsDiff(mpc, (MindMapMapModel)base_map.getModel(), 
				(MindMapMapModel)v1_map.getModel());
		this.base_v2_diff = new MapsDiff(mpc, (MindMapMapModel)base_map.getModel(), 
				(MindMapMapModel)v2_map.getModel());
		
		log.debug("get node list v1");
		this.v1_nodes = MapHelper.getNodeList(this.v1_map.getRootNode());
		log.debug("get node list v2");
		this.v2_nodes = MapHelper.getNodeList(this.v2_map.getRootNode());
		
		this.node_position_list = new HashMap<String, Boolean>();
		for (String node_id : this.v1_nodes.keySet()) {
			MindMapNode node = this.v1_nodes.get(node_id);
			this.node_position_list.put(node_id, node.isLeft());
		}
		for (String node_id : this.v2_nodes.keySet()) {
			if (!this.node_position_list.containsKey(node_id)) {
				MindMapNode node = this.v2_nodes.get(node_id);
				this.node_position_list.put(node_id, node.isLeft());
			}
		}
		
		this.conflict_list = new ConflictList(base_v1_diff, base_v2_diff, v1_nodes, v2_nodes);
		this.merged_map_id_to_real_id = new HashMap<String, String>();
		v1_real_id_index = new HashMap<String, String>();
		v2_real_id_index = new HashMap<String, String>();
		merged_real_id_index = new HashMap<String, String>();
		
		merged_map = (MindMapController)mpc.getController().getMode().createModeController();
		new MindMapMapModel(mpc.getController().getModel().getFrame(), merged_map);
		MindMapNode root_node = merged_map.getRootNode();
		
		this.v1_root_node = merged_map.newChild.addNewNode(root_node, root_node.getChildCount());
		this.v2_root_node = merged_map.newChild.addNewNode(root_node, root_node.getChildCount());
		this.merged_root_node = merged_map.newChild.addNewNode(root_node, root_node.getChildCount());
		
		v1_root_node.setText("V1");
		v2_root_node.setText("V2");
		merged_root_node.setText("Merged");
		
		copyTree(MapType.V1, (MindMapNodeModel)v1_root_node, (MindMapNodeModel)v1_map.getRootNode());
		copyTree(MapType.V2, (MindMapNodeModel)v2_root_node, (MindMapNodeModel)v2_map.getRootNode());
		copyTree(MapType.MERGED, (MindMapNodeModel)merged_root_node, (MindMapNodeModel)base_map.getRootNode());
		
		// apply nonconflicting changes
		Vector<Change> changes_list = base_v1_diff.getChangesList().getList();
		log.debug(changes_list.toString());
		for (Change change : changes_list) {
			if (!change.conflicting) {
				applyChange(MapType.V2, change, 1);
				applyChange(MapType.MERGED, change, 1);
			} else if (change.type == ChangeType.ADDED) {
				applyChange(MapType.MERGED, change, 1);
				change.type = ChangeType.EDITED;
			}
		}
		changes_list = base_v2_diff.getChangesList().getList();
		log.debug(changes_list.toString());
		for (Change change : changes_list) {
			if (!change.conflicting) {
				applyChange(MapType.V1, change, 2);
				applyChange(MapType.MERGED, change, 2);
			} else if (change.type == ChangeType.ADDED) {
				change.type = ChangeType.EDITED;
			}
		}
		this.merged_map.markAsMergingMap(this);
	}
	
	private void copyTree(MapType type, MindMapNodeModel version_node, MindMapNodeModel original_node) {
		MindMapNodeModel temp = (MindMapNodeModel)merged_map.newChild.addNewNode(
				version_node, version_node.getChildCount());
		
		String real_id = original_node.getObjectId(original_node.getModeController());
		String temp_id = temp.getObjectId(merged_map);
		merged_map_id_to_real_id.put(temp_id, real_id);
		if (type == MapType.V1) {
			v1_real_id_index.put(real_id, temp_id);
		} else if (type == MapType.V2) {
			v2_real_id_index.put(real_id, temp_id);
		} else {
			merged_real_id_index.put(real_id, temp_id);
		}
		
		NodeHelper.copyNodeAttributes(original_node, temp);
		int child_count = original_node.getChildCount();
		for (int i = 0; i < child_count; ++i) {
			copyTree(type, temp, (MindMapNodeModel)original_node.getChildAt(i));
		}
	}
	
	private void applyChange(MapType changed_map_type, MapsDiff.ChangeList.Change change, int version) {
		log.debug("changed_map_type " + changed_map_type
				+ ", change " + change.toString() + ", version " + version);
		MindMapController changed_map = version == 1 ? v1_map : v2_map;
		MapsDiff diff = version == 1 ? base_v1_diff : base_v2_diff;
		
		HashMap<String, String> real_id_index;
		if (changed_map_type == MapType.V1) {
			real_id_index = v1_real_id_index;
		} else if (changed_map_type == MapType.V2) {
			real_id_index = v2_real_id_index;
		} else {
			real_id_index = merged_real_id_index;
		}
		
		if (change.type == ChangeType.DELETED) {
			String id = real_id_index.get(change.id);
			if (id != null) {
				merged_map.deleteNode(merged_map.getNodeFromID(id));
				real_id_index.remove(change.id);
				merged_map_id_to_real_id.remove(id);
			}
			return;
		}
		
		if (change.type == ChangeType.PARENT_CHANGED) {
			String new_parent = diff.getParentChangedNodes().get(change.id);
			String temp_id = real_id_index.get(change.id);
			String temp_new_parent_id = real_id_index.get(new_parent);
			merged_map.getNodeFromID(temp_id)
				.setParent(merged_map.getNodeFromID(temp_new_parent_id));
			return;
		}
		
		if (change.type == ChangeType.ADDED) {
			String parent = diff.getAddedNodes().get(change.id);
			if (!real_id_index.containsKey(change.id)) {
				String temp_parent_id = real_id_index.get(parent);
				MindMapNode new_node = changed_map.getNodeFromID(change.id);
				MindMapNode added = merged_map.newChild.addNewNode(
						merged_map.getNodeFromID(temp_parent_id), 
						Math.min(new_node.getParentNode().getChildPosition(new_node),
								merged_map.getNodeFromID(temp_parent_id).getChildCount()));
				real_id_index.put(change.id, added.getObjectId(added.getMap().getModeController()));
				merged_map_id_to_real_id.put(added.getObjectId(added.getMap().getModeController()), change.id);
			}
		}
		
		String temp_id = real_id_index.get(change.id);
		MindMapNode new_node = merged_map.getNodeFromID(temp_id);
		MindMapNode original_node = changed_map.getNodeFromID(change.id);
		NodeHelper.copyNodeAttributes((MindMapNodeModel)original_node, 
				(MindMapNodeModel)new_node);
	}
	
	public MindMapController getMergedMap() {
		return merged_map;
	}
	public ConflictList getConflictList() {
		return conflict_list;
	}
	public MapsDiff getMapDiff1() {
		return base_v1_diff;
	}
	public MapsDiff getMapDiff2() {
		return base_v2_diff;
	}
	
	public MindMapController finalizedMergedMap() {
		StringWriter fileout = new StringWriter();
		try {
			merged_map.getModel().getXml(fileout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("Merged map --- " + fileout.toString());
		log.debug(merged_map_id_to_real_id.toString());
		final MindMapController final_map = 
			(MindMapController)mpc.getController().getMode().createModeController();
		final MindMapMapModel map_model = new MindMapMapModel(mpc.getController().getModel().getFrame(), final_map);
		MapHelper.copyAttributeRegistry(v2_map, final_map);
		MindMapNodeModel merged_root = (MindMapNodeModel) merged_root_node.getChildren().get(0);
		
		String real_root_id = merged_map_id_to_real_id.get(merged_root.getObjectId(merged_map));
		boolean is_left = merged_root.isLeft();
		if (this.node_position_list.containsKey(real_root_id)) {
			is_left = this.node_position_list.get(real_root_id).booleanValue();
		}

		NewNodeAction new_node_action = final_map.newChild.getAddNodeAction(
				final_map.getRootNode(), 0, real_root_id, is_left);
		final_map.newChild.act(new_node_action);
		final_map.getModel().setRoot(final_map.getNodeFromID(real_root_id));
		restoreMergedMapSubtree(merged_root, (MindMapNodeModel)final_map.getRootNode());
		
		return final_map;
	}
	
	private void restoreMergedMapSubtree(MindMapNodeModel merged_root, MindMapNodeModel final_root) {
		NodeHelper.copyNodeAttributes(merged_root, final_root);
		MindMapController final_map = (MindMapController)final_root.getModeController();
		int child_count = merged_root.getChildCount();
		for (int i = 0; i < child_count; ++i) {
			MindMapNodeModel merged_node = ((MindMapNodeModel)merged_root.getChildAt(i));
			String temp_id = merged_node.getObjectId(merged_map);
			String real_id = merged_map_id_to_real_id.containsKey(temp_id) ? 
					merged_map_id_to_real_id.get(temp_id) : temp_id;
			boolean is_left = merged_root.isLeft();
			if (!merged_real_id_index.containsValue(temp_id) && merged_real_id_index.containsKey(real_id)) {
				real_id = temp_id;
			}
			if (this.node_position_list.containsKey(real_id)) {
					is_left = this.node_position_list.get(real_id).booleanValue();
			}
			NewNodeAction new_node_action = final_map.newChild.getAddNodeAction(
					final_root, i, real_id, is_left);
			try {
				final_map.newChild.act(new_node_action);
			} catch (IllegalArgumentException e) {
				log.error(real_id);
				log.error(merged_real_id_index.toString());
				log.error(merged_map_id_to_real_id.toString());
			}
			restoreMergedMapSubtree(merged_node, (MindMapNodeModel)final_root.getChildAt(i));
		}
	}
	
	public void addNodeToMergedMap(String parent, String new_node_id) {
		if (merged_real_id_index.containsValue(parent)) {
			merged_map_id_to_real_id.put(new_node_id, new_node_id);
			merged_real_id_index.put(new_node_id, new_node_id);
		}
	}
	
	public void showMergingMap() {
		File file;
		try {
			String merged_root_id = this.merged_root_node.getObjectId(this.merged_map);
			String v1_root_id = this.v1_root_node.getObjectId(this.merged_map);
			String v2_root_id = this.v2_root_node.getObjectId(this.merged_map);
			file = File.createTempFile("merging", ".mm");
			FileWriter writer = new FileWriter(file);
			merged_map.getModel().getXml(writer);
			writer.close();
			merged_map = (MindMapController) merged_map.load(file.toURI().toURL());
			this.merged_root_node = merged_map.getNodeFromID(merged_root_id);
			this.v1_root_node = merged_map.getNodeFromID(v1_root_id);
			this.v2_root_node = merged_map.getNodeFromID(v2_root_id);
			merged_map.markAsMergingMap(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.conflict_shown = 0;
	}
	
	public boolean showNextConflict() {
		if (this.conflict_shown > 0)
			removeAdditionalIconFromConflictingNode();
		
		if (this.conflict_shown >= conflict_list.getList().size())
			return false;

		Conflict conflict = conflict_list.getList().get(this.conflict_shown++);
		this.conflicting_nodes = new Vector<MindMapNode>();
		
		if (conflict.type == ConflictType.NODE_DELETED_SUBTREE_MODIFIED ||
				conflict.type == ConflictType.PARENT_CHANGES_NODE_DELETED) {
			String id = conflict.id_v1 == null ? conflict.id_v2 : conflict.id_v1;
			conflicting_nodes.add(merged_map.getNodeFromID(merged_real_id_index.get(id)));
			conflicting_nodes.add(merged_map.getNodeFromID(conflict.id_v1 == null ?
					v1_real_id_index.get(id) : v2_real_id_index.get(id)));
		} else if (conflict.type == ConflictType.DIFFERENT_ATTRIBUTES ||
				conflict.type == ConflictType.PARENTS_CHANGE) {
			conflicting_nodes.add(merged_map.getNodeFromID(merged_real_id_index.get(conflict.id_v1)));
			conflicting_nodes.add(merged_map.getNodeFromID(v1_real_id_index.get(conflict.id_v1)));
			conflicting_nodes.add(merged_map.getNodeFromID(v2_real_id_index.get(conflict.id_v1)));
		} else if (conflict.type == ConflictType.CYCLIC_PARENT){
			conflicting_nodes.add(merged_map.getNodeFromID(merged_real_id_index.get(conflict.id_v1)).getParentNode());
			conflicting_nodes.add(merged_map.getNodeFromID(v1_real_id_index.get(conflict.id_v1)));
			conflicting_nodes.add(merged_map.getNodeFromID(v2_real_id_index.get(conflict.id_v2)));
		}
		
		for (MindMapNode node : conflicting_nodes) {
			merged_map.addIcon(node, MindIcon.factory("button_cancel"));
		}
		
		return true;
	}
	
	private void removeAdditionalIconFromConflictingNode() {
		try {
			for (MindMapNode node : this.conflicting_nodes) {
				if (node != null) {
					int index = node.getIcons().indexOf(MindIcon.factory("button_cancel"));
					merged_map.removeLastIconAction.act(
							merged_map.removeLastIconAction.createRemoveIconXmlAction(node, index));
				}
			}
		} catch (IllegalArgumentException e) {
			
		}
	}
}
