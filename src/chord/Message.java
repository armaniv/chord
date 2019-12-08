package chord;

public class Message {
	private MessageType type;
	private Integer key;
	
	public Message(MessageType type) {
		this.setType(type);
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer successor) {
		this.key = successor;
	}
}
