package chord;

public class Message {
	private MessageType type;
	private Integer key;
	
	public Message(MessageType type, Integer key) {
		this.type = type;
		this.key = key;
	}

	public MessageType getType() {
		return this.type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}
	
	public Integer getKey() {
		return this.key;
	}
}
