package chord;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
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
		
		NetworkBuilder<Object> networkBuilder = new NetworkBuilder<Object>("lookup_network", context, false);
		networkBuilder.buildNetwork();
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		int num_nodes = params.getInteger("num_nodes");
		double p_fail = params.getDouble("p_fail");
				
		ChordNode chordNode = new ChordNode(context, space, num_nodes, p_fail);
		context.add(chordNode);
		
		return context;
	}

}
