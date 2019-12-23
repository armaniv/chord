package chord;

import java.util.ArrayList;

public class Message {
	private MessageType type;
	private MessageType subType;
	private Integer key;				//the value we are searching for inside the chord ring (default null)
	private Integer reqId;
	private Integer sourceNode;
	private Integer destinationNode;
	private Integer successor;				//the next node to contact in order to continuing a lookup  (default null)
	private Integer predecessor;			//the predecessor of a node, used in stabilize procedure (default null)
	private ArrayList<Integer> successorList;		// the successorList of a node
	
	public Message(MessageType type, Integer sourceNode, Integer destinationNode) {
		this.setType(type);
		this.setSourceNode(sourceNode);
		this.setDestinationNode(destinationNode);
		this.setSubType(null);
	}

	
	public MessageType getType() {
		return this.type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = Integer.valueOf(key);
	}

	public Integer getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(Integer sourceNode) {
		this.sourceNode = Integer.valueOf(sourceNode);
	}

	public Integer getSuccessor() {
		return successor;
	}

	public void setSuccessor(Integer successor) {
		this.successor = Integer.valueOf(successor);
	}

	public Integer getDestinationNode() {
		return destinationNode;
	}

	public void setDestinationNode(Integer destinationNode) {
		this.destinationNode = Integer.valueOf(destinationNode);
	}

	public Integer getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Integer predecessor) {
		this.predecessor = Integer.valueOf(predecessor);
	}

	public MessageType getSubType() {
		return subType;
	}

	public void setSubType(MessageType subType) {
		this.subType = subType;
	}

	public Integer getReqId() {
		return reqId;
	}

	public void setReqId(Integer reqId) {
		this.reqId = Integer.valueOf(reqId);
	}
	
	public ArrayList<Integer> getSuccessorList() {
		return successorList;
	}

	public void setSuccessorList(ArrayList<Integer> successorList) {
		this.successorList = new ArrayList<>();
		for(int i=0; i < successorList.size(); i++){
			this.successorList.add(Integer.valueOf(successorList.get(i)));
		}
	}

}
