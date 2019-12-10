package chord;

import java.util.ArrayList;

public class Lookup {
	private Integer key;
	private ArrayList<Integer> messagePath;
	private Integer outcome;
	
	public Lookup(Integer key) {
		this.key = key;
		this.outcome = null;
	}
	
	public void addNode(Integer nodeId) {
		this.getMessagePath().add(nodeId);
	}

	public Integer getOutcome() {
		return outcome;
	}

	public void setOutcome(Integer outcome) {
		this.outcome = outcome;
	}
	
	public Integer getKey() {
		return this.key;
	}

	public ArrayList<Integer> getMessagePath() {
		return messagePath;
	}

	public void setMessagePath(ArrayList<Integer> contactedNodes) {
		this.messagePath = contactedNodes;
	}

}
