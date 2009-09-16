package plugins.sharedmind.synchronouscollaboration;

import freemind.modes.mindmapmode.actions.xml.ActionPair;

public class SharedAction implements Cloneable, Comparable<SharedAction> {
    private VectorClock timestamp;
	private ActionPair action_pair;
    private String from;
    private long sum_of_timestamp;
    private boolean undoed;

    public SharedAction(String from, VectorClock timestamp, ActionPair action_pair) {
        this.from = from;
        this.timestamp = timestamp;
        this.action_pair = action_pair;
        this.sum_of_timestamp = 0;
        for (int vc : timestamp.getHashMap().values()) {
        	this.sum_of_timestamp += vc;
        }
        this.undoed = false;
    }

	public void setFrom(String from) {
		this.from = from;
	}

    public String getFrom() {
        return from;
    }
    
    public VectorClock getTimestamp() {
        return timestamp;
    }

    public ActionPair getActionPair() {
        return action_pair;
    }
    
    public SharedAction clone() {
    	return new SharedAction(this.from, this.timestamp.clone(), this.getActionPair());
    }

	@Override
	public int compareTo(SharedAction o) {
		if (!(this.sum_of_timestamp == o.sum_of_timestamp))
			return (new Long(this.sum_of_timestamp)).compareTo(o.sum_of_timestamp);
		else
			return this.from.compareTo(o.from);
		
	}

	public synchronized void setUndoed(boolean undoed) {
		this.undoed = undoed;
	}

	public synchronized boolean isUndoed() {
		return undoed;
	}
	
	public String toString() {
		return sum_of_timestamp + " " + timestamp.toString() + " " + from;
	}
}
