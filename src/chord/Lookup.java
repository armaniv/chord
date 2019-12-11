package chord;

import java.util.ArrayList;

public class Lookup {
	private Integer key;
	private ArrayList<Integer> messagePath;
	
	public Lookup(Integer key) {
		this.key = key;
		this.messagePath = new ArrayList<>();
	}
	
	public void addNodeToPath(Integer nodeId) {
		this.getMessagePath().add(Integer.valueOf(nodeId));
	}
	
	public Integer getKey() {
		return this.key;
	}

	public ArrayList<Integer> getMessagePath() {
		return messagePath;
	}

}
