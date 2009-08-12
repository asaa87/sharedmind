package freemind.modes.mindmapmode.actions.xml;

import java.util.List;

import org.apache.log4j.Logger;

import plugins.MergedMap;
import freemind.controller.Controller;
import freemind.controller.actions.generated.instance.MoveNodesAction;
import freemind.controller.actions.generated.instance.NewNodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.NodeAdapter;

public class MergingActionFactory extends ActionFactory {
	private Logger log = Logger.getLogger(MergingActionFactory.class);
	
	private MergedMap merged_map;
	private Controller c;
	
	public MergingActionFactory(Controller c, MergedMap merged_map) {
		super(c);
		this.c = c;
		this.merged_map = merged_map;
	}

	public boolean executeAction (ActionPair pair) {
		boolean return_value = super.executeAction(pair);
		log.debug("execute: " + pair.getDoAction().toString());
		log.debug(return_value);
		if (return_value) {
			XmlAction do_action = pair.getDoAction();
			if (do_action instanceof NewNodeAction) {
				NewNodeAction new_node_action = (NewNodeAction) do_action;
				NodeAdapter parent = 
					this.c.getModeController().getNodeFromID(new_node_action.getNode());
				merged_map.addNodeToMergedMap(parent.getObjectId(merged_map.getMergedMap()),
						new_node_action.getNewId());
			} else if (do_action instanceof MoveNodesAction) {
				MoveNodesAction move_nodes_action = (MoveNodesAction) do_action;
				String node_id = move_nodes_action.getNode();
				merged_map.moveSubtree(node_id);
				List<String> nodes_id = move_nodes_action.getListNodeListMemberList();
				for (String id : nodes_id)
					merged_map.moveSubtree(id);
			}
		}
		return return_value;
	}
}
