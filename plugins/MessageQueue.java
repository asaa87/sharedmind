package plugins;

import java.util.Vector;
import java.util.Map;
import java.util.Iterator;

import freemind.controller.actions.generated.instance.NodeAction;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

public class MessageQueue implements Cloneable{
    public static class Message implements Cloneable{
        private VectorClock timestamp;
        private ActionPair action_pair;
        private String from;
        
        public Message(String from, VectorClock timestamp, ActionPair action_pair) {
            this.from = from;
            this.timestamp = timestamp;
            this.action_pair= action_pair;
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
        
        public Message clone() {
        	return new Message(this.from, this.timestamp, this.action_pair);
        }
    }
    
    private Vector<Message> queue;
    private VectorClock vector_clock;
    private String user_id;
    private Vector<String> current_participant;
    
    public MessageQueue(String user_id, VectorClock vector_clock) {
        this.queue = new Vector<Message>();
        this.user_id = user_id;
        this.vector_clock = vector_clock;
        this.current_participant = new Vector<String>();
    }
    
//    public boolean conflicting(Message message) {
//    	return false;
////        return this.vector_clock.getClock(user_id) >
////            message.getTimestamp().getClock(user_id);
//    }
    
    private boolean needDelay(Message message, VectorClock max_vector_clock) {
        if (message.timestamp.getClock(message.getFrom()) != 
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
    
    public Vector<Message> enqueueAndReturnAllThatCanBeExecuted(Message message) {
        Vector<Message> return_value = new Vector<Message>();
        if (needDelay(message, vector_clock)) {
            queue.add(message);
        } else {
        	VectorClock max_vector_clock = vector_clock.clone();
            return_value.add(message);
            max_vector_clock.adjustWithTimestamp(message.getTimestamp());
            boolean continue_loop = true;
            while (continue_loop) {
                continue_loop = false;
                Iterator<Message> iter = queue.iterator();
                while (iter.hasNext()) {
                    Message temp = iter.next();
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
        for (Message message : queue) {
            if (message.action_pair.getDoAction() instanceof NodeAction &&
                objectId.equals(((NodeAction) message.action_pair.getDoAction()).getNode()))
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
		clone.queue = (Vector<Message>)this.queue.clone();
		return clone;
	}
	
	public boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public void setCurrentParticipant(Vector<String> participants) {
		this.current_participant = participants;
	}
}
