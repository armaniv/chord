package chord;

public class Message {
	private MessageType type;
	private Integer successor;
	
	public Message(MessageType type) {
		this.setType(type);
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Integer getSuccessor() {
		return successor;
	}

	public void setSuccessor(Integer successor) {
		this.successor = successor;
	}
}
