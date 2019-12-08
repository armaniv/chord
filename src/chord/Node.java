package chord;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;
	private Router router;
	private ChordNode masterNode;

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Router router, ChordNode masterNode) {
		this.id = id;
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
		this.router = router;
		this.masterNode = masterNode;
	}

	public void receive(Message message) {
		switch (message.getType()) {
		case LOOKUP:

			break;

		default:
			// nothing for now (maybe for ever)
			break;
		}
	}

	public Integer findSuccessor(Integer serchedId) {
		// successor contained inside the interval (add + 1 to successor)
		if (insideInterval(serchedId, this.id, successor + 1)) {
			return successor;
		}
		else
		{
			Integer cpNode = closestPrecedingNode(serchedId);
			return cpNode;
		}
	}

	public Integer closestPrecedingNode(Integer serchedId) {
		for (int i = fingerTable.length - 1; i >= 0; i--) {
			int entry = fingerTable[i];
			
			if (insideInterval(entry, this.id, serchedId)) {
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

	public void startLookup(Integer lookupKey) {
		Message msg = new Message(MessageType.LOOKUP, lookupKey);
		Integer destination = findSuccessor(lookupKey);
		
		System.out.println(String.format("%02d[%02d] -> %03d", this.id, lookupKey, destination));
		this.router.send(msg, this.id, destination);
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
