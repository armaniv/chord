package chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import chord.SchedulableActions.MasterRetryLookup;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

public class ChordNode {
	private Integer SPACEDIMENSION = 50000;
	private Integer FINGER_TABLE_SIZE = (int) (Math.log(SPACEDIMENSION) / Math.log(2));

	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Router router;
	private Network<Object> network;
	
	private Integer num_nodes;
	private Integer churn_rate;
	private Random rnd; // Java random, approximately uniform distributed
	private HashMap<Integer, Node> nodes; 
	private HashMap<Integer, ArrayList<RepastEdge<Object>>> edges;
	
	private ArrayList<FindSuccReq> successfulRequests;
	private ArrayList<FindSuccReq> unsuccessfulRequests;

	public ChordNode(Context<Object> context, ContinuousSpace<Object> space, int num_nodes, int churn_rate) {
		this.context = context;
		this.space = space;
		this.network = (Network<Object>) context.getProjection("lookup_network");
		this.router = new Router();
		
		this.num_nodes = num_nodes;
		this.churn_rate = churn_rate;
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

			Node node = new Node(id, FINGER_TABLE_SIZE, router, this, NodeState.SUBSCRIBED);
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
			
			// already subscribed peers
			node.setState(NodeState.SUBSCRIBED);		
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
		while(randomNode.getState() == NodeState.NEW) {
			randomNode = selectRandomNode();
		}
		randomNode.lookup(lookupKey);
	}

	@ScheduledMethod(start = 8, interval = 5, priority = 100)
	public void simulateChurnRate(){
		int n_FailAndJoin = (this.num_nodes * this.churn_rate) / 100;
		
		for(int i=0; i< n_FailAndJoin; i++) {
			Node node = selectRandomNode();		//choose a node randomly
			Integer key = node.getId();			//get its key
			node.removeAllSchedule();			//remove all its scheduled actions
			this.nodes.remove(key);				//remove it from the set of nodes
			this.context.remove(node);			//remove it from the context
			this.router.removeANode(key);		//signal to the router to remove it
			this.edges.remove(key);				//remove it from the hash edges
			System.out.println("Node" + key + " crashes");
		}
		
		for(int i=0; i < n_FailAndJoin; i++) {
			// Generate a new node 
			// call Join() on it
			// add node to nodes 
			// add node to router 
			
			int id = -1;
			while(id == -1 || this.nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}
			Node node = new Node(id, FINGER_TABLE_SIZE, router, this, NodeState.NEW);
			
			this.context.add(node);
			this.router.addNode(node);
			visualizeNode(node);
			Node selNode = selectRandomNode();
			while (selNode.getState() == NodeState.NEW) {
				selNode = selectRandomNode();
			}
			this.nodes.put(id, node);
			
			System.out.println("Node " + id + " joining");
			node.join(selNode.getId());
		}
	}
	
	public void signalSuccessuful(FindSuccReq req) {
		this.successfulRequests.add(req);
	}
	
	public void signalUnsuccessful(FindSuccReq req, Integer resolverNodeId) {
		this.unsuccessfulRequests.add(req);
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
		if(this.edges.get(source) == null) {
			this.edges.put(source, new ArrayList<>());
		}
		
		ArrayList<RepastEdge<Object>> singleNodeEdges = this.edges.get(source);
		if(nodes.get(source) != null && nodes.get(destination)!= null) {
			singleNodeEdges.add(this.network.addEdge(nodes.get(source), nodes.get(destination)));
		}
	}
	
	public void removeAnEdge(Integer source, Integer destination) {
		ArrayList<RepastEdge<Object>> singleNodeEdges = this.edges.get(source);
		
		for (int i = singleNodeEdges.size() -1; i >= 0; i--) {
			RepastEdge<Object> edge = singleNodeEdges.get(i);
			if(edge.getTarget().equals(this.nodes.get(destination))) {
				network.removeEdge(edge);
				singleNodeEdges.remove(i);
			}
		}
	}
}
