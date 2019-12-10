package chord;

public class Message {
	private MessageType type;
	private Integer lookupKey;
	private Integer sourceNode;
	private Integer destinationNode;
	private Integer successor;
	
	public Message(MessageType type, Integer soureNode, Integer destinationNode) {
		this.setType(type);
		this.setSourceNode(sourceNode);
		this.setDestinationNode(destinationNode);
	}

	public MessageType getType() {
		return this.type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Integer getLookupKey() {
		return lookupKey;
	}

	public void setLookupKey(Integer key) {
		this.lookupKey = key;
	}

	public Integer getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(Integer sourceNode) {
		this.sourceNode = sourceNode;
	}

	public Integer getSuccessor() {
		return successor;
	}

	public void setSuccessor(Integer successor) {
		this.successor = successor;
	}

	public Integer getDestinationNode() {
		return destinationNode;
	}

	public void setDestinationNode(Integer destinationNode) {
		this.destinationNode = destinationNode;
	}
}
