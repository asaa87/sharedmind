package plugins.sharedmind;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import freemind.controller.actions.generated.instance.NodeAction;

public class MessageQueue implements Cloneable{
	private static ConcurrentHashMap<String, Integer> InitialVectorClock = 
		new ConcurrentHashMap<String, Integer>();
	
    private Vector<SharedAction> queue;
    private VectorClock vector_clock;
    private String user_id;
    private Vector<String> current_participant;
    private Logger log;
    
    public static int getInitialVectorClock(String user_id) {
		return InitialVectorClock.get(user_id);
    };
    
    public MessageQueue(String user_id, VectorClock vector_clock) {
    	log = Logger.getLogger(this.getClass());
        this.queue = new Vector<SharedAction>();
        this.user_id = user_id;
        this.vector_clock = vector_clock;
        this.current_participant = new Vector<String>();
        InitialVectorClock = 
        	new ConcurrentHashMap<String, Integer>(this.vector_clock.getHashMap());
    }
    
    private boolean needDelay(SharedAction message, VectorClock max_vector_clock) {
    	log.debug(message.getTimestamp().toString());
    	log.debug("max vc: " + max_vector_clock.toString());
        if (message.getTimestamp().getClock(message.getFrom()) != 
            max_vector_clock.getClock(message.getFrom()) + 1) {
            return true;
        } else {
            for (Map.Entry<String, Integer> timestamp : message.getTimestamp()) {
                if (!timestamp.getKey().equals(message.getFrom()) &&
                		timestamp.getValue() > max_vector_clock.getClock(timestamp.getKey())) {
                	return true;
                }
            }
            return false;
        }
    }
    
    public synchronized Vector<SharedAction> enqueueAndReturnAllThatCanBeExecuted(SharedAction message) {
        Vector<SharedAction> return_value = new Vector<SharedAction>();
        if (needDelay(message, vector_clock)) {
            queue.add(message);
        } else {
        	VectorClock max_vector_clock = vector_clock.clone();
            return_value.add(message);
            max_vector_clock.adjustWithTimestamp(message.getTimestamp());
            log.debug(queue.toString());
            boolean continue_loop = true;
            while (continue_loop) {
                continue_loop = false;
                for (SharedAction queued_message : queue) {
                    if (!needDelay(queued_message, max_vector_clock)) {
                        return_value.add(queued_message);
                        max_vector_clock.adjustWithTimestamp(queued_message.getTimestamp());
                        continue_loop = true;
                    }
                }
            	queue.removeAll(return_value);
            }
        }
        log.debug(return_value.toString());
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
			InitialVectorClock.put(entry.getKey(), entry.getValue());
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
	
	public Vector<String> getCurrentParticipant() {
		return this.current_participant;
	}
}
