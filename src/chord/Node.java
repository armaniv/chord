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
	
	public void findSuccessor(Integer id) {
		if (id > this.getId() && id <= this.successor) {
			// I have found who stores that key
		}
	}

	private Integer[] getFingerTable() {
		return fingerTable;
	}

	private void setFingerTable(Integer[] fingerTable) {
		this.fingerTable = fingerTable;
	}

	private Integer getSuccessor() {
		return successor;
	}

	private void setSuccessor(Integer successor) {
		this.successor = successor;
	}

	private Integer getPredecessor() {
		return predecessor;
	}

	private void setPredecessor(Integer predecessor) {
		this.predecessor = predecessor;
	}

	private Integer getId() {
		return id;
	}

	private void setId(Integer id) {
		this.id = id;
	}

}
