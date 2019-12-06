package chord;

import java.util.HashMap;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;

public class ChordBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		// --- Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications
		context.setId("chord");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(), new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50);
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		int NUM_NODES = params.getInteger("num_nodes");
		int FINGER_TABLE_SIZE = (int) (Math.log(NUM_NODES)/Math.log(2));
		
		double spaceSize = space.getDimensions().getHeight();
	    double center = spaceSize / 2;
	    double radius = center / 2;
	    
	    Random rnd = new Random(); // Java random, approximately uniform distributed
	    int spaceDimension = 10000; // the interval from which node id are extracted
	    
	    HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
	    
	    for (int i = 0; i < NUM_NODES; i++) {
	    	int id = rnd.nextInt(spaceDimension);
	    	
	    	while(nodes.containsKey(id)) {
	    		id = rnd.nextInt(spaceDimension);
	    	}
	    	
	    	Node node = new Node(id, FINGER_TABLE_SIZE);
	    	nodes.put(id, node);
	    	context.add(node);
	    	
	    	double theta = 2 * Math.PI * id / spaceDimension;
	        double x = center + radius * Math.cos(theta);
	        double y = center + radius * Math.sin(theta);
	        space.moveTo(node, x, y);
	    }
	    
		return context;
		
	}

}
