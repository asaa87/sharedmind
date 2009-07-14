package plugins;

import java.util.HashMap;

public class MapMessageContent implements MessageContent {
    public String requester;
    public String vector_clock;
    public String map;
    public HashMap<Integer, String> checkpoint_list;
    
    public MapMessageContent(String requester, String vector_clock, String map,
    		HashMap<Integer, String> checkpoint_map_list) {
    	this.requester = requester;
    	this.vector_clock = vector_clock;
    	this.map = map;
    	this.checkpoint_list = checkpoint_map_list;
    }
}
