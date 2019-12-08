package chord;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;
	private Router router;
	private ChordNode masterNode;
	private Message processedMessage;

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Router router, ChordNode masterNode) {
		this.id = id;
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
		this.router = router;
		this.masterNode = masterNode;
	}

	public void receive(Message message) {
		this.processedMessage = message;
		this.processedMessage.incrementReceivers();
		
		switch (message.getType()) {
			case LOOKUP:
				this.onLookup(message);
				break;
	
			default:
				// nothing for now (maybe for ever)
				break;
		}
	}
	
	public void onLookup(Message message) {
		Integer successor = findSuccessor(message.getLookupKey());
		if (successor != null) {
			signalKeyFoundAt(successor);
		}
	}

	public Integer findSuccessor(Integer id) {
		// successor contained inside the interval (add + 1 to successor)
		if (insideInterval(id, this.id, successor + 1)) {
			return successor;
		}
		else
		{
			Integer cpNode = closestPrecedingNode(id);
			// this node doesn't have the key -> forward LOOKUP message
			this.router.send(this.processedMessage, this.id, cpNode);
			return null;
		}
	}

	public Integer closestPrecedingNode(Integer id) {
		for (int i = fingerTable.length - 1; i >= 0; i--) {
			int entry = fingerTable[i];
			
			if (insideInterval(entry, this.id, id)) {
				return entry;
			}
		}
		return this.id;
	}

	private boolean insideInterval(Integer value, Integer a, Integer b) {
		if (value > a && value < b) {
			return true;
		}
		if (value < a && a > b && value < b) {
			return true;
		}
		if (value > b && a > b && value > a) {
			return true;
		}
		
		return false;
	}

	
	private void signalKeyFoundAt(Integer nodeId) {
		this.masterNode.receiveLookupResult(this.processedMessage, nodeId);
	}
	
	
	// --------------- getter and setter methods ---------------
	
	public Integer[] getFingerTable() {
		return fingerTable;
	}

	public void setFingerTable(Integer[] fingerTable) {
		this.fingerTable = fingerTable;
	}

	public void setSuccessor(Integer successor) {
		this.successor = successor;
	}

	public void setPredecessor(Integer predecessor) {
		this.predecessor = predecessor;
	}

	public Integer getId() {
		return id;
	}
	
}
