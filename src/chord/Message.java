package chord;

public class Message {
	private MessageType type;
	private Integer lookupKey;
	private Integer receiversCount;
	
	public Message(MessageType type, Integer lookupKey) {
		this.type = type;
		this.lookupKey = lookupKey;
		this.receiversCount = 0;
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

	public void setlookupKey(Integer key) {
		this.lookupKey = key;
	}
	
	public void incrementReceivers() {
		this.receiversCount++;
	}
}
