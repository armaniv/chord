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
