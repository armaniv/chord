package chord;

import java.util.ArrayList;
import java.util.Arrays;

import chord.SchedulableActions.FailCheck;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private Integer successor;
	private Integer predecessor;
	private Integer next;
	private Router router;
	private ChordNode masterNode;
	private PendingFindSuccReq pendingFindSuccReq;
	private ArrayList<ISchedulableAction> actions = new ArrayList<>();

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Router router, ChordNode masterNode) {
		this.id = id;
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
		this.router = router;
		this.masterNode = masterNode;
		this.pendingFindSuccReq = new PendingFindSuccReq();
		this.next = 0;
	}

	public void receive(Message message) {
		
		switch (message.getType()) {			
		case FIND_SUCC:
			//System.out.println("Node " + this.id.toString() + " received FIND_SUCC("+message.getLookupKey()+") from " + message.getSourceNode());
			this.onFindSucc(message);
			break;
		case FOUND_KEY:
			//System.out.println("Node " + this.id.toString() + " received FOUND_KEY("+message.getSourceNode()+") from " + message.getSourceNode());
			this.onFoundKey(message);
			break;
		case FOUND_SUCC:
			//System.out.println("Node " + this.id.toString() + " received FOUND_SUCC("+message.getSuccessor()+") from " + message.getSourceNode());
			this.onFoundSucc(message);
			break;
		case STABILIZE:
			//System.out.println("Node " + this.id.toString() + " received STABILIZE from " + message.getSourceNode());
			this.onStabilize(message);
			break;
		case ACK_STABILIZE:
			//System.out.println("Node " + this.id.toString() + " received ACK_STABILIZE from " + message.getSourceNode());
			this.onACKStabilize(message);
			break;
		case NOTIFY:
			//System.out.println("Node " + this.id.toString() + " received NOTIFY from " + message.getSourceNode());
			this.onNotify(message);
			break;
		default:
			// nothing for now (maybe for ever), throw Exception?
			break;
		}
	}
	
	public void onFindSucc(Message message) {
		if (insideInterval(message.getKey(), this.predecessor, this.id+1)) {
			Message ack = new Message(MessageType.FOUND_KEY, this.id, message.getSourceNode());
			ack.setKey(message.getKey());
			ack.setSubType(message.getSubType());
			ack.setSuccessor(this.id);
			ack.setReqId(message.getReqId());
			router.send(ack);
			System.out.println("Node " + this.id.toString() + " sent FOUND_KEY("+message.getKey()+") to " + message.getSourceNode().toString());
		}else {
			Integer successor = findSuccessor(message.getKey());
			Message ack = new Message(MessageType.FOUND_SUCC, this.id, message.getSourceNode());
			ack.setKey(message.getKey());
			ack.setSubType(message.getSubType());
			ack.setSuccessor(successor);
			ack.setReqId(message.getReqId());
			this.router.send(ack);
			System.out.println("Node " + this.id.toString() + " sent FOUND_SUCC("+ack.getSuccessor()+") to " + message.getSourceNode().toString());
		}
	}
	
	// forward the request
	// schedule timeout check
	public void onFoundSucc(Message message) {
		Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, message.getSuccessor());
		findSuccMsg.setSubType(message.getSubType());
		findSuccMsg.setKey(message.getKey());
		findSuccMsg.setReqId(message.getReqId());
		sendFindSucc(findSuccMsg);
		this.masterNode.removeAnEdge(this.id, message.getSourceNode());
		System.out.println("Node " + this.id.toString() + " sent FIND_SUCC(" + message.getKey() +") to " + message.getSuccessor());
	}
	
	// remove pendingLookup and deliver it to master
	public void onFoundKey(Message message) {
		FindSuccReq request = this.pendingFindSuccReq.removeRequest(message.getReqId());
		ArrayList<Integer> messagePath = request.getMessagePath();
		switch (message.getSubType()){
		case LOOKUP:
			this.signalLookupResolved(request);
			System.out.println("Key " + request.getFindSuccKey() + " found at Node " + messagePath.get(messagePath.size()-1) + 
					" in " + messagePath.size() + " steps " + Arrays.toString(messagePath.toArray()) );
			break;
		case JOIN:
			this.successor = message.getSuccessor();
			System.out.println("Node " + this.id + " JOINS with successor=" + message.getSuccessor() + " ; MsgPath: " + Arrays.toString(messagePath.toArray()));
			break;
		case FIX_FINGERS:
			break;
		default:
			// should never be here? throw Exception?
			break;
		}
		this.masterNode.removeAnEdge(this.id, message.getSourceNode());
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
			if (fingerTable[i] == null) continue;
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

	public void lookup(Integer findSuccKey) {
		if (insideInterval(findSuccKey, this.predecessor, this.id+1)) {
			FindSuccReq findSuccReq = new FindSuccReq(findSuccKey);
			findSuccReq.addNodeToPath(this.id);
			signalLookupResolved(findSuccReq);
			System.out.println("Node " + this.id.toString() + " resolved FIND_SUCC(" +findSuccKey+") by ITSELF ");
		}else {	
			Integer successor = findSuccessor(findSuccKey);
			Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, successor);
			findSuccMsg.setSubType(MessageType.LOOKUP);
			findSuccMsg.setKey(findSuccKey);
			sendFindSucc(findSuccMsg);
			System.out.println("Node " + this.id.toString() + " sent FIND_SUCC(" + findSuccMsg.getKey() +") to " + findSuccMsg.getDestinationNode().toString());
		}
	}
	
	public void sendFindSucc(Message findSuccMsg) {
		FindSuccReq findSuccReq = this.pendingFindSuccReq.getRequest(findSuccMsg.getReqId());
		if (findSuccReq == null) {
			findSuccReq = new FindSuccReq(findSuccMsg.getKey());
		}
		Integer destNodeId = findSuccMsg.getDestinationNode();
		findSuccReq.addNodeToPath(destNodeId);
		findSuccMsg.setReqId(findSuccReq.getId());
		this.pendingFindSuccReq.addRequest(findSuccReq);
		scheduleFailCheck(findSuccReq, destNodeId);
		this.router.send(findSuccMsg);
		this.masterNode.visualizeAnEdge(this.id, destNodeId);
	}
	
	private void signalLookupResolved(FindSuccReq lookup) {
		this.masterNode.receiveLookupResult(lookup);
	}
	
	
	private void scheduleFailCheck(FindSuccReq request, Integer destinationNodeId) {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters scheduleParameters = 
				ScheduleParameters.createOneTime(schedule.getTickCount() + 5, PriorityType.RANDOM);
		this.actions.add(schedule.schedule(scheduleParameters, new FailCheck(this, request.getId(), destinationNodeId)));
	}
	
	public void removeAllSchedule() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		for(int i = 0; i < this.actions.size(); i++) {
			schedule.removeAction(this.actions.get(i));
		}
	}
	
	public void failCheck(Integer lookupKey, Integer nodeIdToCheck) {
		// UNCOMPLETED
		if (this.pendingFindSuccReq.isPathBroken(nodeIdToCheck, lookupKey)) {
			System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> CRASHED");
		}else {
			System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> OK");
		}
	}
	
	public void join(Integer nodeId) {
		this.predecessor = null;
		Message joinMessage = new Message(MessageType.FIND_SUCC, this.id, nodeId);
		joinMessage.setKey(this.id);
		joinMessage.setSubType(MessageType.JOIN);
		sendFindSucc(joinMessage);
		System.out.println("Node " + this.id.toString() + " sends FIND_SUCC(JOIN,"+this.id.toString()+") to " + nodeId.toString());
	}
	
	@ScheduledMethod(start = 5, interval = 5)
	public void fixFingers() {
		if (this.successor == null) return; // still JOINing
		this.next++;
		if (this.next > this.fingerTable.length) {
			this.next = 1;
		}
		Integer key = this.id + (int) Math.pow(2, next-1);
		Integer succ = findSuccessor(key);
		if (succ.equals(this.id)) {
			succ = this.successor;
		}
		Message fixFingersMessage = new Message(MessageType.FIND_SUCC, this.id, succ);
		fixFingersMessage.setSubType(MessageType.FIX_FINGERS);
		fixFingersMessage.setKey(key);
		sendFindSucc(fixFingersMessage);
		System.out.println("Node " + this.id + " sends FIND_SUCC(FIX_FINGERS,"+key+") to " + succ);
	}
	
	@ScheduledMethod(start = 5, interval = 5)
	public void stabilize(){
		if (this.successor != null) {
			Message msgStabilize = new Message(MessageType.STABILIZE, this.id, this.successor);
			this.router.send(msgStabilize);		
		}
	}
	
	public void onStabilize(Message message) {
		Message replayStabilize = new Message(MessageType.ACK_STABILIZE, this.id, message.getSourceNode());
		replayStabilize.setPredecessor(this.predecessor);
		this.router.send(replayStabilize);
	}
	
	public void onACKStabilize(Message message) {
		Integer x = message.getPredecessor();
		if(insideInterval(x, this.id, this.successor)){
			this.successor = x;
		}
		
		//successor.notify(n);
		Message notifyMsg = new Message(MessageType.NOTIFY, this.id, this.successor);
		this.router.send(notifyMsg);
	}
	
	public void onNotify(Message message) {
		Integer nPrime = message.getSourceNode();
		
		if (this.predecessor == null || insideInterval(nPrime, this.predecessor, this.id)) {
			this.predecessor = nPrime;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Node))
			return false;
		Node o = (Node) obj;
		return o.getId().equals(this.id);
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
