package chord;

import java.util.ArrayList;

public class FindSuccReq {
	private Integer findSuccKey;
	private Integer id;
	private ArrayList<Integer> messagePath;
	private int next;
	private static int C=0;
	
	public FindSuccReq(Integer key)  {
		setFindSuccKey(key);
		setId(C++);
		this.messagePath = new ArrayList<>();
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

}
