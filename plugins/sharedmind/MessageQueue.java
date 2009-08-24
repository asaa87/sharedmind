package plugins.sharedmind;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import freemind.controller.actions.generated.instance.NodeAction;

public class MessageQueue implements Cloneable{
    private Vector<SharedAction> queue;
    private VectorClock vector_clock;
    private String user_id;
    private Vector<String> current_participant;
    
    public MessageQueue(String user_id, VectorClock vector_clock) {
        this.queue = new Vector<SharedAction>();
        this.user_id = user_id;
        this.vector_clock = vector_clock;
        this.current_participant = new Vector<String>();
    }
    
    private boolean needDelay(SharedAction message, VectorClock max_vector_clock) {
        if (message.getTimestamp().getClock(message.getFrom()) != 
            max_vector_clock.getClock(message.getFrom()) + 1) {
            return true;
        } else {
            for (Map.Entry<String, Integer> timestamp : message.getTimestamp()) {
                if (!timestamp.getKey().equals(message.getFrom()) &&
                    timestamp.getValue() > max_vector_clock.getClock(timestamp.getKey()))
                        return true;
            }
        }
        return false;
    }
    
    public Vector<SharedAction> enqueueAndReturnAllThatCanBeExecuted(SharedAction message) {
        Vector<SharedAction> return_value = new Vector<SharedAction>();
        if (needDelay(message, vector_clock)) {
            queue.add(message);
        } else {
        	VectorClock max_vector_clock = vector_clock.clone();
            return_value.add(message);
            max_vector_clock.adjustWithTimestamp(message.getTimestamp());
            boolean continue_loop = true;
            while (continue_loop) {
                continue_loop = false;
                Iterator<SharedAction> iter = queue.iterator();
                while (iter.hasNext()) {
                    SharedAction temp = iter.next();
                    if (!needDelay(message, max_vector_clock)) {
                        iter.remove();
                        return_value.add(temp);
                        max_vector_clock.adjustWithTimestamp(temp.getTimestamp());
                        continue_loop = true;
                    }
                }
            }
        }
        return return_value;
    }
    
    public VectorClock getVectorClock() {
        return vector_clock;
    }

    public boolean editConflicting(String objectId) {
        for (SharedAction message : queue) {
            if (message.getActionPair().getDoAction() instanceof NodeAction &&
                objectId.equals(((NodeAction) message.getActionPair().getDoAction()).getNode()))
                return true;
        }
        return false;
    }

	public void addCollaborators(String user_and_clock) {
		VectorClock temp = new VectorClock(user_and_clock);
		for (Map.Entry<String, Integer> entry : temp) {
			vector_clock.addCollaborator(entry.getKey(), entry.getValue());
		}
	}

	public boolean hasMaxVectorClock(String exception) {
		int local_clock = this.vector_clock.getClock(this.user_id);
        for (Map.Entry<String, Integer> entry : this.vector_clock) {
            if (current_participant.contains(entry.getKey())
            		&& !entry.getKey().equals(this.user_id)
                    && !entry.getKey().equals(exception)
            		&& local_clock < entry.getValue())
            	return false;
        }
        return true;
	}
	
	public MessageQueue clone() {
		MessageQueue clone = new MessageQueue(this.user_id, this.vector_clock);
		clone.vector_clock = (VectorClock)this.vector_clock.clone();
		clone.queue = (Vector<SharedAction>)this.queue.clone();
		return clone;
	}
	
	public boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public void setCurrentParticipant(Vector<String> participants) {
		this.current_participant = participants;
	}
}
