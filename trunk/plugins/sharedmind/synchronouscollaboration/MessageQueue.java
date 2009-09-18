package plugins.sharedmind.synchronouscollaboration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import plugins.sharedmind.MapSharingController;

import freemind.controller.actions.generated.instance.NodeAction;

public class MessageQueue implements Cloneable{
	private static ConcurrentHashMap<String, Integer> InitialVectorClock = 
		new ConcurrentHashMap<String, Integer>();
	
    private Vector<SharedAction> queue;
    private VectorClock vector_clock;
    private String user_id;
    private Vector<String> current_participant;
    private Logger log;
    private LostMessages lost_messages;
    private VectorClock max_vector_clock;
    private MapSharingController mpc;
    
    public static int getInitialVectorClock(String user_id) {
		return InitialVectorClock.get(user_id);
    };
    
    public MessageQueue(MapSharingController mpc, String user_id, VectorClock vector_clock) {
    	log = Logger.getLogger(this.getClass());
        this.queue = new Vector<SharedAction>();
        this.user_id = user_id;
        this.vector_clock = vector_clock;
        this.current_participant = new Vector<String>();
        this.lost_messages = new LostMessages();
        this.max_vector_clock = vector_clock.clone();
        this.mpc = mpc;
        InitialVectorClock = 
        	new ConcurrentHashMap<String, Integer>(this.vector_clock.getHashMap());
    }
    
    private void updateMaxVectorClock(SharedAction message) {
    	int current_clock = max_vector_clock.getClock(message.getFrom());
    	int message_timestamp = message.getTimestamp().getClock(message.getFrom()); 
    	max_vector_clock.getHashMap().put(message.getFrom(), 
    			Math.max(current_clock, message_timestamp));
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
    	
        if (discardMessage(message))
    		return return_value;
    	
        if (needDelay(message, vector_clock)) {
            queue.add(message);
            int max_vc = this.max_vector_clock.getClock(message.getFrom());
            int msg_vc = message.getTimestamp().getClock(message.getFrom());
            log.warn("max vc: " + max_vector_clock.toString());
            for (int i = max_vc + 1; i < msg_vc ; ++i) {
            	this.lost_messages.addLostMessage(message.getFrom(), i);
            	mpc.sendRequestRetransmissionMessage(message.getFrom(), i);
            }
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
        
        this.lost_messages.tryRemoveFromLostMessage(message);
        updateMaxVectorClock(message);
        
        return return_value;
    }
    
    public synchronized boolean discardMessage(SharedAction message) {
		if (this.vector_clock.getClock(message.getFrom()) <
			message.getTimestamp().getClock(message.getFrom())) {
			return false;
		} else if (this.lost_messages.isLostMessage(message)) {
			return false;
		}
    	log.warn("message discarded : " + message.getFrom() + " " + 
    			message.getTimestamp().getClock(message.getFrom()));
		return true;
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
		MessageQueue clone = new MessageQueue(this.mpc, this.user_id, this.vector_clock);
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
