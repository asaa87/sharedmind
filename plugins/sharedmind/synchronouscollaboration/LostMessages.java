package plugins.sharedmind.synchronouscollaboration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class LostMessages {
    private Map<String, Vector<Integer>> lost_messages;
    
    public LostMessages() {
    	this.lost_messages = 
    		Collections.synchronizedMap(new HashMap<String, Vector<Integer>>());
    }
    
    
    public synchronized void addLostMessage(String sender_id, int clock_value) {
    	if (!lost_messages.containsKey(sender_id)) {
    		lost_messages.put(sender_id, new Vector<Integer>());
    	}
    	if (!lost_messages.get(sender_id).contains(clock_value)) {
    		lost_messages.get(sender_id).add(clock_value);
    	}
    }
    
    public synchronized void tryRemoveFromLostMessage(SharedAction shared_action) {
    	String sender_id = shared_action.getFrom();
    	Integer clock_value = shared_action.getTimestamp().getClock(sender_id);
    	if (lost_messages.containsKey(sender_id)) {
    		lost_messages.get(sender_id).remove(clock_value);
    	}
    }

    public synchronized boolean isLostMessage(SharedAction shared_action) {
    	String sender_id = shared_action.getFrom();
    	Integer clock_value = shared_action.getTimestamp().getClock(sender_id);
    	return lost_messages.containsKey(sender_id) && 
    			lost_messages.get(sender_id).contains(clock_value);
    }
}
