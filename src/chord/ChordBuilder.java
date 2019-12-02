package chord;

import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
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

		Random rnd = new Random();
		
		double spaceSize = space.getDimensions().getHeight();
	    double center = spaceSize / 2;
	    double radius = center / 2;

	    for (int i = 0; i < 100; i++) {
	    	int id = rnd.nextInt(1000);
	    	Node nodo = new Node(id);
	    	context.add(nodo);
	    	double theta = 2 * Math.PI * id / 1000;
	        double x = center + radius * Math.cos(theta);
	        double y = center + radius * Math.sin(theta);
	        space.moveTo(nodo, x, y);
	    }
	    
		return context;
		
	}

}
