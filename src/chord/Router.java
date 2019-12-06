package chord;

import java.util.HashMap;

public class Router {
	private HashMap<Integer, Node> nodes; 

	public Router() {
	}

	public void setNodes(HashMap<Integer, Node> nodes) {
		this.nodes = nodes;
	}
	
	public void send(Message gossip, Integer sourceNodeId, Integer destinationNodeId) {

	}


	public Node locateNode(Integer id) {
		return this.nodes.get(id);
	}
}
