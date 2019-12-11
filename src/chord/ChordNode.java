package chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;

public class ChordNode {

	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Integer SPACEDIMENSION = 64;
	private Integer FINGER_TABLE_SIZE = (int) (Math.log(SPACEDIMENSION) / Math.log(2));
	private Random rnd; // Java random, approximately uniform distributed
	private Router router;
	HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

	public ChordNode(Context<Object> context, ContinuousSpace<Object> space, int num_init_nodes, int num_total_nodes) {
		this.context = context;
		this.space = space;
		this.rnd = new Random();
		this.router = new Router();
		createInitialNetwork(num_init_nodes);
	}

	private void createInitialNetwork(int num_init_nodes) {
		for (int i = 0; i < num_init_nodes; i++) {
			int id = rnd.nextInt(SPACEDIMENSION);

			while (nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}

			Node node = new Node(id, FINGER_TABLE_SIZE, router, this);
			this.nodes.put(id, node);
			this.context.add(node);
			visualizeNode(node);
		}
		
		this.router.setNodes(nodes);
		createInitialFingerTables();
	}

	private void createInitialFingerTables() {
		List<Integer> sortedKeys = new ArrayList<>(nodes.keySet());
		Collections.sort(sortedKeys);
		System.out.println(Arrays.toString(sortedKeys.toArray()));

		int counter = 1;
		Integer predecessor = null;
		Node firstNode = null;
		for (Integer key : sortedKeys) {
			Node node = nodes.get(key);
			int id = node.getId();
			Integer[] fingerTable = new Integer[FINGER_TABLE_SIZE];

			for (int i = 0; i < FINGER_TABLE_SIZE; i++) {
				int value = id + (int) Math.pow(2, i);

				for (int j = 0; j < sortedKeys.size(); j++) {
					if (sortedKeys.get(j) >= (value % SPACEDIMENSION)) {
						fingerTable[i] = sortedKeys.get(j);
						break;
					}
				}
				if (fingerTable[i] == null) {
					fingerTable[i] = sortedKeys.get(0);
				}
			}

			// System.out.println(id + ": " + Arrays.toString(fingerTable));
			node.setFingerTable(fingerTable);
			node.setSuccessor(fingerTable[0]);
			
			// set predecessor
			if (counter == 1) {
				firstNode = node;
			}else if (counter == nodes.size()) {
				node.setPredecessor(predecessor);
				firstNode.setPredecessor(node.getId());
			}else {
				node.setPredecessor(predecessor);
			}
			
			predecessor = node.getId();
			counter++;
		}
	}

	private void visualizeNode(Node node) {
		double spaceSize = space.getDimensions().getHeight();
		double center = spaceSize / 2;
		double radius = center / 2;

		double theta = 2 * Math.PI * node.getId() / SPACEDIMENSION;
		double x = center + radius * Math.cos(theta);
		double y = center + radius * Math.sin(theta);
		space.moveTo(node, x, y);
	}
	
	// generate a lookup(key, node) only if the node 
	// is not already processing the same request
	@ScheduledMethod(start = 1, interval = 0)
	public void generateLookup() {
		Node randomNode = null;
		int lookupKey = rnd.nextInt(SPACEDIMENSION);
		Boolean isAlreadyProcessingThisKey = Boolean.TRUE;
		while (isAlreadyProcessingThisKey) {
			randomNode = selectRandomNode();
			isAlreadyProcessingThisKey = randomNode.isAlreadyProcessingLookupFor(lookupKey);
		}
		randomNode.startLookup(lookupKey);
	}
	
	public void receiveLookupResult(Lookup lookup) {
		ArrayList<Integer> messagePath = lookup.getMessagePath();
		System.out.println("Key " + lookup.getKey() + " found at Node " + messagePath.get(messagePath.size()-1));
	}
	
	public Node selectRandomNode() {
		int selectedNode = rnd.nextInt(nodes.size() - 1);
		Integer[] nodes = new Integer[this.nodes.size()];
		nodes = this.nodes.keySet().toArray(nodes);
		return this.nodes.get(nodes[selectedNode]);
	}
}
