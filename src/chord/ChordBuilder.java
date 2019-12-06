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
		int num_nodes = params.getInteger("num_nodes");
		
		ChordNode chordNode = new ChordNode(context, space, num_nodes);
		
	    
		return context;
		
	}

}
