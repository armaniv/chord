package chord;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;

	public Node(Integer id, Integer FINGER_TABLE_SIZE) {
		this.setId(id);
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
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
		//// !!!!!!! --- successor contained inside the interval?????
		if (insideInterval(serchedId, this.id, successor)) {
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

	private boolean insideInterval(Integer id, Integer a, Integer b) {
		
		if (id > a && id < b) {
			return true;
		}
		if (id < a && a > b && id < b) {
			return true;
		}
		if (id > b && a > b && id > a) {
			return true;
		}
		
		return false;
	}

	public Integer[] getFingerTable() {
		return fingerTable;
	}

	public void setFingerTable(Integer[] fingerTable) {
		this.fingerTable = fingerTable;
	}

	public Integer getSuccessor() {
		return successor;
	}

	public void setSuccessor(Integer successor) {
		this.successor = successor;
	}

	public Integer getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Integer predecessor) {
		this.predecessor = predecessor;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
