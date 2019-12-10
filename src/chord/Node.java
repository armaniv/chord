package chord;

import chord.SchedulableActions.FailCheck;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;
	private Router router;
	private ChordNode masterNode;
	private Message processedMessage;
	private PendingLookups pendingLookups;
	private Boolean isCrashed;

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Router router, ChordNode masterNode) {
		this.id = id;
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
		this.router = router;
		this.masterNode = masterNode;
		this.pendingLookups = new PendingLookups();
		this.isCrashed = Boolean.FALSE;
	}

	public void receive(Message message) {
		this.processedMessage = message;
		
		switch (message.getType()) {
				
			case LOOKUP:
				this.onLookup(message);
				break;
				
			case FOUND_KEY:
				// remove pendingLookup and deliver it to master
				break;
				
			case FOUND_SUCC:
				this.onFoundSucc(message);
				break;
	
			default:
				// nothing for now (maybe for ever)
				break;
		}
	}
	
	public void onLookup(Message message) {
		if (insideInterval(message.getLookupKey(), this.predecessor, this.id)) {
			Message ack = new Message(MessageType.FOUND_KEY, message.getSourceNode());
			ack.setLookupKey(message.getLookupKey());
			ack.setSuccessor(this.id);
			sendMessage(ack, message.getSourceNode());
		}else {
			Integer successor = findSuccessor(message.getLookupKey());
			Message ack = new Message(MessageType.FOUND_SUCC, message.getSourceNode());
			ack.setLookupKey(message.getLookupKey());
			ack.setSuccessor(successor);
			this.router.send(ack, this.id, successor);
		}
		
	}
	
	public void onFoundSucc(Message message) {
		// update pendingLookups
		// forward the request
		// schedule timeout check
		
	}

	public Integer findSuccessor(Integer id) {
		// successor contained inside the interval (add + 1 to successor)
		if (insideInterval(id, this.id, successor + 1)) {
			return successor;
		}
		else
		{
			Integer cpNode = closestPrecedingNode(id);
			return cpNode;
		}
	}

	public Integer closestPrecedingNode(Integer id) {
		for (int i = fingerTable.length - 1; i >= 0; i--) {
			int entry = fingerTable[i];
			
			if (insideInterval(entry, this.id, id)) {
				return entry;
			}
		}
		return this.id;
	}

	private boolean insideInterval(Integer value, Integer a, Integer b) {
		if (value > a && value < b) {
			return true;
		}
		if (value < a && a > b && value < b) {
			return true;
		}
		if (value > b && a > b && value > a) {
			return true;
		}
		
		return false;
	}

	public void startLookup(Integer lookupKey) {
		if (insideInterval(lookupKey, this.predecessor, this.id)) {
			Lookup lookup = new Lookup(lookupKey);
			lookup.addNode(this.id);
			lookup.setOutcome(this.id);
			signalLookupResolved(lookup);
		}else {
			Message lookupMessage = new Message(MessageType.LOOKUP, lookupKey);
			Integer successor = findSuccessor(lookupKey);
			sendLookup(lookupMessage, successor);
		}
	}
	
	public void sendLookup(Message lookupMessage, Integer destinationNodeId) {
		Lookup lookup = new Lookup(lookupMessage.getLookupKey());
		lookup.addNode(destinationNodeId);
		this.pendingLookups.addLookup(lookup);
		scheduleFailCheck(lookup, destinationNodeId);
		this.router.send(lookupMessage, this.id, destinationNodeId);
		System.out.println("Node " + this.id.toString() + " sent LOOKUP to " + destinationNodeId.toString());
	}
	
	private void signalLookupResolved(Lookup lookUp) {
		this.masterNode.receiveLookupResult(lookUp);
	}
	
	private void sendMessage(Message message, Integer dest) {
		this.router.send(message, this.id, dest);
	}
	
	private void scheduleFailCheck(Lookup lookup, Integer destinationNodeId) {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters scheduleParameters = 
				ScheduleParameters.createOneTime(schedule.getTickCount() + 5, PriorityType.RANDOM);
		schedule.schedule(scheduleParameters, new FailCheck(this, lookup, destinationNodeId));
	}
	
	public void failCheck(Lookup lookup, Integer nodeIdToCheck) {
		// UNCOMPLETED
		if (this.pendingLookups.isPathBroken(nodeIdToCheck, lookup)) {
			
		}
	}
	
	public Boolean isCrashed() {
		return this.isCrashed;
	}
	
	// master should check this before sending LOOKUP requests to nodes
	public Boolean isAlreadyProcessingLookupFor(Integer key) {
		return this.pendingLookups.containsLookupFor(key);
	}
	
	
	// --------------- getter and setter methods ---------------
	
	public Integer[] getFingerTable() {
		return fingerTable;
	}

	public void setFingerTable(Integer[] fingerTable) {
		this.fingerTable = fingerTable;
	}

	public void setSuccessor(Integer successor) {
		this.successor = successor;
	}

	public void setPredecessor(Integer predecessor) {
		this.predecessor = predecessor;
	}
	
	public Integer getPredecessor() {
		return this.predecessor;
	}

	public Integer getId() {
		return id;
	}
	
}
