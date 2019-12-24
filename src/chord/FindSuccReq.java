package chord;

import java.util.ArrayList;

public class FindSuccReq {
	private Integer findSuccKey;
	private Integer id;
	private ArrayList<Integer> messagePath;
	private ArrayList<ArrayList<Integer>> brokenPaths;
	private MessageType type;
	private int next;
	private int maxRetry;

	public FindSuccReq(Integer key, Integer counter) {
		setFindSuccKey(key);
		setId(counter);
		this.messagePath = new ArrayList<>();
		this.brokenPaths = new ArrayList<ArrayList<Integer>>();
		this.maxRetry = 5;
	}

	public void addNodeToPath(Integer nodeId) {
		this.getMessagePath().add(Integer.valueOf(nodeId));
	}

	public ArrayList<Integer> getMessagePath() {
		return messagePath;
	}

	public Integer getFindSuccKey() {
		return findSuccKey;
	}

	private void setFindSuccKey(Integer findSuccKey) {
		this.findSuccKey = findSuccKey;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public int getMaxRetry() {
		return maxRetry;
	}

	public void prepareForRetry() {
		this.maxRetry = this.maxRetry - 1;
		int originatorNodeId = this.messagePath.get(0);
		this.brokenPaths.add(this.messagePath);
		this.messagePath = new ArrayList<Integer>();
		this.messagePath.add(originatorNodeId);
	}

	public ArrayList<ArrayList<Integer>> getBrokenPaths() {
		return brokenPaths;
	}
	
	public int getPathLength() {
		return this.messagePath.size();
	}

}
