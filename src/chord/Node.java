package chord;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;
	
	public Node() {
		
	}
	
	public Node(Integer id) {
		this.id = id;
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

}
