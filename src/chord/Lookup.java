package chord;

import java.util.ArrayList;

public class Lookup {
	private Integer key;
	private ArrayList<Integer> messagePath;
	private Integer outcome;
	
	public Lookup(Integer key) {
		this.key = key;
		this.outcome = null;
		this.messagePath = new ArrayList<>();
	}
	
	public void addNodeToPath(Integer nodeId) {
		this.getMessagePath().add(Integer.valueOf(nodeId));
	}

	public Integer getOutcome() {
		return outcome;
	}

	public void setOutcome(Integer outcome) {
		this.outcome = Integer.valueOf(outcome);
	}
	
	public Integer getKey() {
		return this.key;
	}

	public ArrayList<Integer> getMessagePath() {
		return messagePath;
	}

}
