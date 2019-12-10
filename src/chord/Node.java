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
		
		switch (message.getType()) {
				
			case LOOKUP:
				System.out.println("Node " + this.id.toString() + " received LOOKUP("+message.getLookupKey()+") from " + message.getSourceNode());
				this.onLookup(message);
				break;
				
			case FOUND_KEY:
				System.out.println("Node " + this.id.toString() + " received FOUND_KEY("+message.getSourceNode()+") from " + message.getSourceNode());
				this.onFoundKey(message);
				break;
				
			case FOUND_SUCC:
				System.out.println("Node " + this.id.toString() + " received FOUND_SUCC("+message.getSuccessor()+") from " + message.getSourceNode());
				this.onFoundSucc(message);
				break;
	
			default:
				// nothing for now (maybe for ever)
				break;
		}
	}
	
	public void onLookup(Message message) {
		if (insideInterval(message.getLookupKey(), this.predecessor, this.id+1)) {
			Message ack = new Message(MessageType.FOUND_KEY, this.id, message.getSourceNode());
			ack.setLookupKey(message.getLookupKey());
			ack.setSuccessor(this.id);
			router.send(ack);
			System.out.println("Node " + this.id.toString() + " sent FOUND_KEY("+message.getLookupKey()+") to " + message.getSourceNode().toString());
		}else {
			Integer successor = findSuccessor(message.getLookupKey());
			Message ack = new Message(MessageType.FOUND_SUCC, this.id, message.getSourceNode());
			ack.setLookupKey(message.getLookupKey());
			ack.setSuccessor(successor);
			this.router.send(ack);
			System.out.println("Node " + this.id.toString() + " sent FOUND_SUCC("+ack.getSuccessor()+") to " + message.getSourceNode().toString());
		}
		
	}
	
	// update pendingLookups
	// forward the request
	// schedule timeout check
	public void onFoundSucc(Message message) {
		Lookup lookup = this.pendingLookups.getLookup(message.getLookupKey());
		// lookup cannot be null
		lookup.addNodeToPath(message.getSourceNode());
		this.pendingLookups.updateLookup(lookup);
		Message lookupMessage = new Message(MessageType.LOOKUP, this.id, message.getSuccessor());
		lookupMessage.setLookupKey(message.getLookupKey());
		sendLookup(lookupMessage);
		System.out.println("Node " + this.id.toString() + " sent LOOKUP(" + message.getLookupKey() +") to " + message.getSuccessor());
	}
	
	// remove pendingLookup and deliver it to master
	public void onFoundKey(Message message) {
		Lookup lookup = this.pendingLookups.removeLookup(message.getLookupKey());
		this.signalLookupResolved(lookup);
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
		if (value > a && value <= b) {
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
		if (insideInterval(lookupKey, this.predecessor, this.id+1)) {
			Lookup lookup = new Lookup(lookupKey);
			lookup.addNodeToPath(this.id);
			lookup.setOutcome(this.id);
			signalLookupResolved(lookup);
			System.out.println("Node " + this.id.toString() + " resolved LOOKUP(" +lookupKey+") by ITSELF ");
		}else {
			
			Integer successor = findSuccessor(lookupKey);
			Message lookupMessage = new Message(MessageType.LOOKUP, this.id, successor);
			lookupMessage.setLookupKey(lookupKey);
			sendLookup(lookupMessage);
			System.out.println("Node " + this.id.toString() + " sent LOOKUP(" + lookupMessage.getLookupKey() +") to " + lookupMessage.getDestinationNode().toString());
		}
	}
	
	public void sendLookup(Message lookupMessage) {
		Lookup lookup = this.pendingLookups.getLookup(lookupMessage.getLookupKey());
		if (lookup == null) {
			lookup = new Lookup(lookupMessage.getLookupKey());
		}
		Integer destNodeId = lookupMessage.getDestinationNode();
		lookup.addNodeToPath(destNodeId);
		this.pendingLookups.addLookup(lookup);
		scheduleFailCheck(lookup, destNodeId);
		this.router.send(lookupMessage);
	}
	
	private void signalLookupResolved(Lookup lookup) {
		this.masterNode.receiveLookupResult(lookup);
	}
	
	
	private void scheduleFailCheck(Lookup lookup, Integer destinationNodeId) {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters scheduleParameters = 
				ScheduleParameters.createOneTime(schedule.getTickCount() + 5, PriorityType.RANDOM);
		schedule.schedule(scheduleParameters, new FailCheck(this, lookup, destinationNodeId));
	}
	
	public void failCheck(Lookup lookup, Integer nodeIdToCheck) {
		// UNCOMPLETED
		if (this.pendingLookups.isPathBroken(nodeIdToCheck, lookup.getKey())) {
			System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> CRASHED");
		}else {
			System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> OK");
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
