package chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

public class ChordNode {
	private Integer SPACEDIMENSION = Integer.MAX_VALUE;
	private Integer FINGER_TABLE_SIZE = (int) (Math.log(SPACEDIMENSION) / Math.log(2));
	private Integer SUCCESSOR_TABLE_SIZE;

	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Router router;
	private Network<Object> network;

	private Integer num_nodes;
	private Double p_fail;
	private Random rnd; // Java random, approximately uniform distributed
	private HashMap<Integer, Node> nodes;
	private HashMap<Integer, ArrayList<RepastEdge<Object>>> edges;

	private ArrayList<FindSuccReq> successfulRequests;
	private ArrayList<FindSuccReq> unsuccessfulRequests;

	public ChordNode(Context<Object> context, ContinuousSpace<Object> space, int num_nodes, double p_fail) {
		this.context = context;
		this.space = space;
		this.network = (Network<Object>) context.getProjection("lookup_network");
		this.router = new Router();

		this.num_nodes = num_nodes;
		this.SUCCESSOR_TABLE_SIZE = 2 * (int) (Math.log(num_nodes) / Math.log(2)); // 2*log N
		this.p_fail = p_fail;
		this.rnd = new Random();
		this.nodes = new HashMap<Integer, Node>();
		this.edges = new HashMap<Integer, ArrayList<RepastEdge<Object>>>();
		this.successfulRequests = new ArrayList<FindSuccReq>();
		this.unsuccessfulRequests = new ArrayList<FindSuccReq>();
		createInitialNetwork(num_nodes);
	}

	private void createInitialNetwork(int num_init_nodes) {
		for (int i = 0; i < num_init_nodes; i++) {
			int id = rnd.nextInt(SPACEDIMENSION);

			while (nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}

			Node node = new Node(id, FINGER_TABLE_SIZE, SUCCESSOR_TABLE_SIZE, router, this, NodeState.SUBSCRIBED);
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

			node.setFingerTable(fingerTable);

			ArrayList<Integer> successorList = new ArrayList<>();
			int startIndex = sortedKeys.indexOf(key) + 1;

			for (int i = 0; i < SUCCESSOR_TABLE_SIZE; i++) {
				if (startIndex == sortedKeys.size()) {
					startIndex = 0;
				}
				successorList.add(sortedKeys.get(startIndex));
				startIndex++;
			}

			node.setSuccessorList(successorList);

			// set predecessor
			if (counter == 1) {
				firstNode = node;
			} else if (counter == nodes.size()) {
				node.setPredecessor(predecessor);
				firstNode.setPredecessor(node.getId());
			} else {
				node.setPredecessor(predecessor);
			}

			// already subscribed peers
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

	// generate a random lookup(key, node)
	// node is the node responsible for the lookup
	@ScheduledMethod(start = 1, interval = 40)
	public void generateLookup() {
		Node randomNode = selectRandomNode();
		int lookupKey = rnd.nextInt(SPACEDIMENSION);
		while (randomNode.getState() == NodeState.NEW) {
			randomNode = selectRandomNode();
		}
		randomNode.lookup(lookupKey);
	}

	@ScheduledMethod(start = 3, interval = 600)
	public void simulateChurnRate() {
		int n_FailAndJoin = (int) (this.num_nodes * this.p_fail);

		for (int i = 0; i < n_FailAndJoin; i++) {
			Node node = selectRandomNode(); // choose a node randomly
			Integer key = node.getId(); 	// get its key
			node.removeAllSchedule(); 		// remove all its scheduled actions
			this.nodes.remove(key); 		// remove it from the set of nodes
			this.context.remove(node); 		// remove it from the context
			this.router.removeANode(key); 	// signal to the router to remove it
			this.edges.remove(key); 		// remove it from the hash edges
		}

		for (int i = 0; i < n_FailAndJoin; i++) {
			int id = -1;
			while (id == -1 || this.nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}
			
			// Generate a new node
			Node node = new Node(id, FINGER_TABLE_SIZE, SUCCESSOR_TABLE_SIZE, router, this, NodeState.NEW);

			this.context.add(node); 	// add it to the context
			this.router.addNode(node); 	// add it to the router
			visualizeNode(node); 		// visualize it on the display

			Node selNode = selectRandomNode();
			while (selNode.getState() == NodeState.NEW) {
				selNode = selectRandomNode();
			}
			this.nodes.put(id, node); 	// add it to nodes

			node.join(selNode.getId()); // call Join() on it
		}
	}

	public void signalSuccessuful(FindSuccReq req) {
		this.successfulRequests.add(req);

		// In order to compute table mean path with failure during stabilization
		/*
		 * if(this.successfulRequests.size()==1000) {
		 * computeDuringStabilizationNodeFailureExpResults(); }
		 */
	}

	public void signalUnsuccessful(FindSuccReq req, Integer resolverNodeId) {
		this.unsuccessfulRequests.add(req);
	}

	public void signalUnseccessfulJoin(Node node) {
		Node selNode = selectRandomNode();
		while (selNode.getState() == NodeState.NEW) {
			selNode = selectRandomNode();
		}
		node.join(selNode.getId());
	}

	public Node selectRandomNode() {
		int selectedNode = rnd.nextInt(nodes.size() - 1);
		Integer[] nodes = new Integer[this.nodes.size()];
		nodes = this.nodes.keySet().toArray(nodes);
		return this.nodes.get(nodes[selectedNode]);
	}

	public void visualizeAnEdge(Integer source, Integer destination) {
		if (this.edges.get(source) == null) {
			this.edges.put(source, new ArrayList<>());
		}

		ArrayList<RepastEdge<Object>> singleNodeEdges = this.edges.get(source);
		if (nodes.get(source) != null && nodes.get(destination) != null) {
			singleNodeEdges.add(this.network.addEdge(nodes.get(source), nodes.get(destination)));
		}
	}

	public void removeAnEdge(Integer source, Integer destination) {
		ArrayList<RepastEdge<Object>> singleNodeEdges = this.edges.get(source);

		if (singleNodeEdges != null) {
			for (int i = singleNodeEdges.size() - 1; i >= 0; i--) {
				RepastEdge<Object> edge = singleNodeEdges.get(i);
				if (edge.getTarget().equals(this.nodes.get(destination))) {
					network.removeEdge(edge);
					singleNodeEdges.remove(i);
				}
			}
		}
	}

	// need to disable simulateChurnRate() when running this experiment
	// @ScheduledMethod(start = 3, interval = 0)
	public void simultaneousNodeFailures() {
		int n_FailAndJoin = (int) (this.num_nodes * 0);

		for (int i = 0; i < n_FailAndJoin; i++) {
			Node node = selectRandomNode(); // choose a node randomly
			Integer key = node.getId(); 	// get its key
			node.removeAllSchedule(); 		// remove all its scheduled actions
			this.nodes.remove(key); 		// remove it from the set of nodes
			this.context.remove(node); 		// remove it from the context
			this.router.removeANode(key); 	// signal to the router to remove it
			this.edges.remove(key); 		// remove it from the hash edges
		}

		for (int i = 0; i < 10000; i++) {
			Node randomNode = selectRandomNode();
			int lookupKey = rnd.nextInt(SPACEDIMENSION);
			while (randomNode.getState() == NodeState.NEW) {
				randomNode = selectRandomNode();
			}
			randomNode.lookup(lookupKey);
		}
	}

	// @ScheduledMethod(start = 50, interval = 100)
	public void computeSimultaneousNodeFailureExpResults() {
		double tmp = 0;
		double tmp2 = 0;
		for (int i = 0; i < this.successfulRequests.size(); i++) {
			tmp += this.successfulRequests.get(i).getPathLength() - 1;
			tmp2 += this.successfulRequests.get(i).getBrokenPaths().size();
		}
		System.out.println("Mean Path Length for " + this.successfulRequests.size() + " lookups: "
				+ tmp / this.successfulRequests.size());
		System.out.println("Mean Num. of Timeouts for " + this.successfulRequests.size() + " lookups: "
				+ tmp2 / this.successfulRequests.size());

		if (RunEnvironment.getInstance().getCurrentSchedule().getTickCount() > 1000) {
			System.out.println(this.successfulRequests.toString());
		}
	}

	public void computeDuringStabilizationNodeFailureExpResults() {
		double tmp = 0;
		double tmp2 = 0;
		for (int i = 0; i < this.successfulRequests.size(); i++) {
			tmp += this.successfulRequests.get(i).getPathLength() - 1;
			tmp2 += this.successfulRequests.get(i).getBrokenPaths().size();
		}
		System.out.println("Mean Path Length for " + this.successfulRequests.size() + " lookups: "
				+ tmp / this.successfulRequests.size());
		System.out.println("Mean Num. of Timeouts for " + this.successfulRequests.size() + " lookups: "
				+ tmp2 / this.successfulRequests.size());
	}

	// @ScheduledMethod(start = 1)
	public void evaluateKeyDistribution() {
		ArrayList<Integer> sortedKeys = new ArrayList<>(nodes.keySet());
		Collections.sort(sortedKeys);
		ArrayList<Integer> numKeys = new ArrayList<>();

		for (int i = sortedKeys.size() - 1; i >= 0; i--) {
			if (i > 0) {
				int numkey = Integer.valueOf(sortedKeys.get(i)) - Integer.valueOf(sortedKeys.get(i - 1));
				numKeys.add(numkey);
			} else {
				int numkey = Integer.valueOf(sortedKeys.get(i))
						+ (this.SPACEDIMENSION - Integer.valueOf(sortedKeys.get(sortedKeys.size() - 1)));
				numKeys.add(numkey);
			}
		}

		double total = 0;
		for (int i = 0; i < numKeys.size(); i++) {
			total = total + numKeys.get(i);
		}

		double avg = total / numKeys.size();

		int firstPerc = computePercentile(1, numKeys);
		int ninetyNinthPerc = computePercentile(99, numKeys);

		// In order to computer AVG_keysxnode.txt
		// System.out.println(this.SPACEDIMENSION + ";" + sortedKeys.size() + ";" + avg
		// + ";" + firstPerc + ";" + ninetyNinthPerc);

		// In order to computer PDF_keysxnode.txt
		/*
		 * for(int i=0; i<numKeys.size(); i++ ) { System.out.println(numKeys.get(i)); }
		 */
	}

	public int computePercentile(int percentile, ArrayList<Integer> list) {
		Collections.sort(list);

		int value;

		double index = list.size() * (percentile / 100.0);
		if (index % 1 == 0) {
			value = (Integer.valueOf(list.get((int) index)) + Integer.valueOf(list.get((int) index))) / 2;
		} else {
			Math.round(index);
			value = Integer.valueOf(list.get((int) index - 1));
		}
		return value;
	}

}
