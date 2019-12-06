package chord;

import java.util.HashMap;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;

public class ChordNode {

	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Integer num_nodes;
	private Integer SPACEDIMENSION = 20000;
	HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
	
	int FINGER_TABLE_SIZE;
	
	public ChordNode(Context<Object> context, ContinuousSpace<Object> space, int num_nodes) {
		this.context = context;
		this.space = space;
		this.num_nodes = num_nodes;
		this.FINGER_TABLE_SIZE = (int) (Math.log(num_nodes)/Math.log(2));
		
		createInitialNetwork();
	}
	
	private void createInitialNetwork() {
	    Random rnd = new Random(); // Java random, approximately uniform distributed
	   
	    for (int i = 0; i < num_nodes; i++) {
	    	int id = rnd.nextInt(SPACEDIMENSION);
	    	
	    	while(nodes.containsKey(id)) {
	    		id = rnd.nextInt(SPACEDIMENSION);
	    	}
	    	
	    	Node node = new Node(id, FINGER_TABLE_SIZE);
	    	nodes.put(id, node);
	    	context.add(node);
	    	
	    	visualizeNode(node);
	    }
	}
	
	private void visualizeNode(Node node){
		double spaceSize = space.getDimensions().getHeight();
	    double center = spaceSize / 2;
	    double radius = center / 2;
	    
	    double theta = 2 * Math.PI * node.getId() / SPACEDIMENSION;
        double x = center + radius * Math.cos(theta);
        double y = center + radius * Math.sin(theta);
        space.moveTo(node, x, y);
	    
	}
	
	
}
