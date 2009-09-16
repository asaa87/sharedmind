package plugins.sharedmind.synchronouscollaboration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class VectorClock implements Iterable<Map.Entry<String, Integer>>, Cloneable {
    protected HashMap<String, Integer> vector_clock;
    
    public VectorClock() {
        this.vector_clock = new HashMap<String, Integer>();
    }
    
    // vector_string is the string generated from VectorClock.getString()
    // it is in the format of "{userid1=0, userid2=1, ... }"
    public VectorClock(String vector_string) {
        this();
        stringToVectorClock(vector_string);
    }
    
    protected void stringToVectorClock(String vector_string) {
        String[] vectorEntry = vector_string.substring(1, vector_string.length() - 1).split(", ");
        for (int i = 0; i < vectorEntry.length; i++) {
            String[] pair = vectorEntry[i].split("=");
            this.vector_clock.put(pair[0], Integer.parseInt(pair[1]));
        }
    }
    
    public String getString() {
        return this.vector_clock.toString();
    }

    public Iterator<Entry<String, Integer>> iterator() {
        return vector_clock.entrySet().iterator();
    }
    
    public synchronized void incrementClock(String user_id) {
        vector_clock.put(user_id, vector_clock.get(user_id) + 1);
    }
    
    public int getClock(String user_id) {
        return this.vector_clock.containsKey(user_id) ? 
        		vector_clock.get(user_id) : MessageQueue.getInitialVectorClock(user_id);
    }
    
    public void addCollaborator(String user_id, int clock) {
        vector_clock.put(user_id, clock);
    }
    
    public String toString() {
        return vector_clock.toString();
    }
    
    public synchronized void adjustWithTimestamp(VectorClock timestamp) {
        for (Entry<String, Integer> entry : timestamp) {
            vector_clock.put(entry.getKey(), Math.max(vector_clock.get(entry.getKey()), entry.getValue()));
        }
    }
    
    public VectorClock clone() {
    	VectorClock clone = new VectorClock();
        clone.vector_clock.putAll(this.vector_clock);
    	return clone;
    }

	public HashMap<String, Integer> getHashMap() {
		return this.vector_clock;
	}
	
    public synchronized boolean isConcurrent(VectorClock vc) {
    	return !(this.happensBefore(vc)) && !(vc.happensBefore(this));
    }
  
    public synchronized boolean happensBefore(VectorClock vc) {
    	for (String user_id : this.vector_clock.keySet()) {
	    	if (vc.getClock(user_id) < this.getClock(user_id))
	    		return false;
	    }
	    return true;
    }
}
