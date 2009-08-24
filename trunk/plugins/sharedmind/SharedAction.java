package plugins.sharedmind;

import freemind.modes.mindmapmode.actions.xml.ActionPair;

public class SharedAction implements Cloneable, Comparable<SharedAction> {
    private VectorClock timestamp;
	private ActionPair action_pair;
    private String from;
    private int sum_of_timestamp;
    private boolean undoed;

    public SharedAction(String from, VectorClock timestamp, ActionPair action_pair) {
        this.from = from;
        this.timestamp = timestamp;
        this.action_pair = action_pair;
        for (int vc : timestamp.vector_clock.values()) {
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
    	return new SharedAction(this.from, this.timestamp, this.getActionPair());
    }

	@Override
	public int compareTo(SharedAction o) {
		if (!(this.sum_of_timestamp == o.sum_of_timestamp))
			return this.sum_of_timestamp - o.sum_of_timestamp;
		else
			return this.from.compareTo(o.from);
		
	}

	public void setUndoed(boolean undoed) {
		this.undoed = undoed;
	}

	public boolean isUndoed() {
		return undoed;
	}
}
