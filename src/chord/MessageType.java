package chord;

public enum MessageType {
	// MainTypes
	FIND_SUCC,
	FOUND_KEY,
	FOUND_SUCC,
	STABILIZE,
	ACK_STABILIZE, 
	NOTIFY,
	
	// SubTypes of FIND_SUCC
	LOOKUP,
	JOIN,
	FIX_FINGERS
}