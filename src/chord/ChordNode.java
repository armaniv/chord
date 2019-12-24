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

			// System.out.println(id + ": " + Arrays.toString(fingerTable));
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

			// System.out.println(key + ": " + Arrays.toString(successorList.toArray()));
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
	@ScheduledMethod(start = 1, interval = 5)
	public void generateLookup() {
		Node randomNode = selectRandomNode();
		int lookupKey = rnd.nextInt(SPACEDIMENSION);
		while (randomNode.getState() == NodeState.NEW) {
			randomNode = selectRandomNode();
		}
		randomNode.lookup(lookupKey);
	}

	@ScheduledMethod(start = 3, interval = 32)
	public void simulateChurnRate() {
		int n_FailAndJoin = (int) (this.num_nodes * this.p_fail);

		for (int i = 0; i < n_FailAndJoin; i++) {
			Node node = selectRandomNode(); 	// choose a node randomly
			Integer key = node.getId(); 		// get its key
			node.removeAllSchedule(); 			// remove all its scheduled actions
			this.nodes.remove(key); 			// remove it from the set of nodes
			this.context.remove(node); 			// remove it from the context
			this.router.removeANode(key); 		// signal to the router to remove it
			this.edges.remove(key); 			// remove it from the hash edges
			// System.out.println("Node" + key + " crashes");
		}

		for (int i = 0; i < n_FailAndJoin; i++) {
			// Generate a new node
			int id = -1;
			while (id == -1 || this.nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}

			Node node = new Node(id, FINGER_TABLE_SIZE, SUCCESSOR_TABLE_SIZE, router, this, NodeState.NEW);

			this.context.add(node); 		// add it to the context
			this.router.addNode(node); 		// add it to the router
			visualizeNode(node); 			// visualize it on the display

			Node selNode = selectRandomNode();
			while (selNode.getState() == NodeState.NEW) {
				selNode = selectRandomNode();
			}
			this.nodes.put(id, node); 		// add it to nodes

			// System.out.println("Node " + id + " joining");
			node.join(selNode.getId()); 	// call Join() on it
		}
	}

	public void signalSuccessuful(FindSuccReq req) {
		this.successfulRequests.add(req);
		int tmp = 0;
		if(this.successfulRequests.size()==1000) {
			for(int i=0; i< this.successfulRequests.size(); i++){
				tmp+= this.successfulRequests.get(i).getPathLength();
			}
			
			System.out.println("Avg first 1000 req: " + tmp/1000.0);
		}
		
		

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

	public void retryLookup(Integer nodeId, Integer key) {
		Node node = this.nodes.get(nodeId);
		if (node != null) {
			node.lookup(key);
		}
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
}
