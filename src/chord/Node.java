package chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import chord.SchedulableActions.FailCheck;
import chord.SchedulableActions.StabilizeFailCheck;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

public class Node {
	private Integer id;
	private Integer[] fingerTable;
	private ArrayList<Integer> successorList;
	private Integer FINGER_TABLE_SIZE;
	private Integer SUCCESSOR_TABLE_SIZE;
	private Integer predecessor;
	private Integer next;
	private Router router;
	private ChordNode masterNode;
	private PendingFindSuccReq pendingFindSuccReq;
	private ArrayList<ISchedulableAction> actions = new ArrayList<>();
	private NodeState state;
	private Integer counter = 0;
	private Integer lastStabilizeId = -1;

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Integer SUCCESSOR_TABLE_SIZE, Router router, ChordNode masterNode, NodeState state) {
		this.id = id;
		this.FINGER_TABLE_SIZE = FINGER_TABLE_SIZE;
		this.fingerTable = new Integer[FINGER_TABLE_SIZE];
		this.SUCCESSOR_TABLE_SIZE = SUCCESSOR_TABLE_SIZE;
		this.successorList = new ArrayList<>();
		this.router = router;
		this.masterNode = masterNode;
		this.pendingFindSuccReq = new PendingFindSuccReq();
		this.next = 0;
		this.setState(state);
	}

	public void receive(Message message) {
				
		switch (message.getType()) {			
		case FIND_SUCC:
			//System.out.println("Node " + this.id.toString() + " received FIND_SUCC("+message.getSubType()+","+message.getKey()+") from " + message.getSourceNode());
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
			//System.out.println("Node " + this.id.toString() + " sent FOUND_KEY("+message.getKey()+") to " + message.getSourceNode().toString());
		}else {
			Integer successor = findSuccessor(message.getKey());
			//if (successor.equals(this.id)) successor = this.successor;
			Message ack = new Message(MessageType.FOUND_SUCC, this.id, message.getSourceNode());
			ack.setKey(message.getKey());
			ack.setSubType(message.getSubType());
			ack.setSuccessor(successor);
			ack.setReqId(message.getReqId());
			this.router.send(ack);
			//System.out.println("Node " + this.id.toString() + " sent FOUND_SUCC("+ack.getSuccessor()+") to " + message.getSourceNode().toString());
		}
	}
	
	public Integer findSuccessor(Integer id) {
		if (insideInterval(id, this.id, getFirtSuccesor() + 1)) {
			return getFirtSuccesor();
		}
		else
		{
			Integer cpNode = closestPrecedingNode(id);
			return cpNode;
		}
	}
	
	public Integer closestPrecedingNode(Integer id) {
		ArrayList<Integer> fullTable = getFullTable();
		for (int i = fullTable.size() - 1; i >= 0; i--) {
			int entry = fullTable.get(i);
			
			if (insideInterval(entry, this.id, id)) {
				return entry;
			}
		}
		return getFirtSuccesor();
	}
	
	private ArrayList<Integer> getFullTable() {
		HashSet<Integer> hs = new HashSet<Integer>();
		hs.addAll(this.successorList);
		
		for (int i = this.fingerTable.length - 1; i >= 0; i--) {
			if (fingerTable[i] == null) continue;
			hs.add(fingerTable[i]);
		}
		
		ArrayList<Integer> fullTable = new ArrayList<>();
		fullTable.addAll(hs);
		
		fullTable.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer arg0, Integer arg1) {
				int dist1 = distance(id, arg0);
				int dist2 = distance(id, arg1);
				return dist1 -dist2;
			}
		});
		
		//System.out.println(this.id + "fullTab: " + Arrays.toString(fullTable.toArray()));
		return fullTable;
	}
	
	public int distance(Integer a, Integer b){
		if(b >= a) return b-a;
		return b+(int)Math.pow(2, this.FINGER_TABLE_SIZE)-a;
		
	}
	// forward the request
	// schedule timeout check
	public void onFoundSucc(Message message) {
		Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, message.getSuccessor());
		findSuccMsg.setSubType(message.getSubType());
		findSuccMsg.setKey(message.getKey());
		findSuccMsg.setReqId(message.getReqId());
		sendFindSucc(findSuccMsg);
		if (findSuccMsg.getSubType().equals(MessageType.LOOKUP)){
			this.masterNode.removeAnEdge(this.id, message.getSourceNode());
		}
		//System.out.println("Node " + this.id.toString() + " sent FIND_SUCC(" + message.getKey() +") to " + message.getSuccessor());
	}
	
	// remove pendingLookup and deliver it to master
	public void onFoundKey(Message message) {
		FindSuccReq request = this.pendingFindSuccReq.removeRequest(message.getReqId());
		ArrayList<Integer> messagePath = request.getMessagePath();
		switch (message.getSubType()){
		case LOOKUP:
			this.masterNode.signalSuccessuful(request);
			System.out.println("Key " + request.getFindSuccKey() + " found at Node " + messagePath.get(messagePath.size()-1) + 
					" in " + messagePath.size() + " steps " + Arrays.toString(messagePath.toArray()) );
			this.masterNode.removeAnEdge(this.id, message.getSourceNode());
			break;
		case JOIN:
			this.successorList.add(0, message.getSuccessor());
			System.out.println("Node " + this.id + " JOINS with succ=" + message.getSuccessor() + "; MsgPath: " + Arrays.toString(messagePath.toArray()));
			break;
		case FIX_FINGERS:
			Integer succ = message.getSuccessor();
			Integer next = request.getNext();
			Integer old = this.fingerTable[next];
			this.fingerTable[next] = succ;
			//System.out.println("Node " + this.id + " FIXED FingerTable[" + next + "]=" + old + " -> " + succ);
			break;
		default:
			// should never be here. throw Exception?
			break;
		}
	}

	private boolean insideInterval(Integer value, Integer a, Integer b) {		
		if (value == null || a == null || b == null) {
			return false;
		}
		
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
			FindSuccReq findSuccReq = new FindSuccReq(findSuccKey, this.counter++);
			findSuccReq.addNodeToPath(this.id);
			this.masterNode.signalSuccessuful(findSuccReq);
			//System.out.println("Node " + this.id.toString() + " resolved FIND_SUCC(LOOKUP," +findSuccKey+") by ITSELF ");
		}else {	
			Integer successor = findSuccessor(findSuccKey);
			Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, successor);
			findSuccMsg.setSubType(MessageType.LOOKUP);
			findSuccMsg.setKey(findSuccKey);
			sendFindSucc(findSuccMsg);
			//System.out.println("Node " + this.id.toString() + " sent FIND_SUCC(LOOKUP," + findSuccMsg.getKey() +") to " + findSuccMsg.getDestinationNode().toString());
		}
	}
	
	public void sendFindSucc(Message findSuccMsg) {
		FindSuccReq findSuccReq = this.pendingFindSuccReq.getRequest(findSuccMsg.getReqId());
		if (findSuccReq == null) {
			findSuccReq = new FindSuccReq(findSuccMsg.getKey(), this.counter++);
		}
		Integer destNodeId = findSuccMsg.getDestinationNode();
		findSuccReq.addNodeToPath(destNodeId);
		findSuccMsg.setReqId(findSuccReq.getId());
		if (findSuccMsg.getSubType().equals(MessageType.FIX_FINGERS)){
			findSuccReq.setNext(this.next);
		}
		this.pendingFindSuccReq.addRequest(findSuccReq);
		scheduleFailCheck(findSuccReq, destNodeId);
		this.router.send(findSuccMsg);
		
		if (findSuccMsg.getSubType().equals(MessageType.LOOKUP)){
			this.masterNode.visualizeAnEdge(this.id, destNodeId);
		}
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
	
	public void failCheck(Integer reqId, Integer nodeIdToCheck) {
		// TODO: DISCRIMINATE IF NODE FAILED DURING JOIN OR AFTER
		
		if (this.pendingFindSuccReq.isPathBroken(nodeIdToCheck, reqId)) {
			FindSuccReq unsuccessfulReq = this.pendingFindSuccReq.getRequest(reqId);
			this.masterNode.signalUnsuccessful(unsuccessfulReq, this.id);
			//System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> CRASHED");
			//System.out.println("FindSucc(" + unsuccessfulReq.getFindSuccKey() + ") FAILED");
		}else {
			//System.out.println("Node " + this.id.toString() + " does CHECK_FAILURE("+nodeIdToCheck+") -> OK");
		}
	}
	
	public void join(Integer nodeId) {
		this.predecessor = null;
		Message joinMessage = new Message(MessageType.FIND_SUCC, this.id, nodeId);
		joinMessage.setKey(this.id);
		joinMessage.setSubType(MessageType.JOIN);
		sendFindSucc(joinMessage);
		//System.out.println("Node " + this.id.toString() + " sends FIND_SUCC(JOIN,"+this.id.toString()+") to " + nodeId.toString());
	}
	
	@ScheduledMethod(start = 4, interval = 6)
	public void fixFingers() {
		if (getFirtSuccesor() == null) return; // still JOINing
		this.next++;
		if (this.next >= this.fingerTable.length) {
			this.next = 1;
		}
		Integer key = (this.id + (int) Math.pow(2, next-1)) % (int) Math.pow(2, fingerTable.length) ;
		Integer succ = findSuccessor(key);
		if (succ.equals(this.id)) {
			succ = getFirtSuccesor();
		}
		Message fixFingersMessage = new Message(MessageType.FIND_SUCC, this.id, succ);
		fixFingersMessage.setSubType(MessageType.FIX_FINGERS);
		fixFingersMessage.setKey(key);
		sendFindSucc(fixFingersMessage);
		//System.out.println("Node " + this.id + " sends FIND_SUCC(FIX_FINGERS,"+key+") to " + succ);
	}
	
	@ScheduledMethod(start = 4, interval = 6)
	public void stabilize(){
		if (getFirtSuccesor() != null) {
			Message msgStabilize = new Message(MessageType.STABILIZE, this.id, getFirtSuccesor());
			this.lastStabilizeId = this.counter++;
			msgStabilize.setReqId(lastStabilizeId);
			this.router.send(msgStabilize);
			
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			ScheduleParameters scheduleParameters = 
					ScheduleParameters.createOneTime(schedule.getTickCount() + 5, PriorityType.RANDOM);
			this.actions.add(schedule.schedule(scheduleParameters, new StabilizeFailCheck(this)));
		}
	}
	
	public void onStabilize(Message message) {
		Message replayStabilize = new Message(MessageType.ACK_STABILIZE, this.id, message.getSourceNode());
		replayStabilize.setReqId(message.getReqId());
		if(this.predecessor != null) {
			replayStabilize.setPredecessor(this.predecessor);
		}
		replayStabilize.setSuccessorList(this.successorList);
		this.router.send(replayStabilize);
	}
	
	public void onACKStabilize(Message message) {
		Integer x = message.getPredecessor();
		Integer ACKId = message.getReqId();
		
		if(this.lastStabilizeId.equals(ACKId)) {
			this.lastStabilizeId = null;
		}
		
		//System.out.println(this.id + "p: " + Arrays.toString(successorList.toArray()));
		
		MergeSuccessorList(message.getSuccessorList());
		
		System.out.println(this.id + "d: " + Arrays.toString(successorList.toArray()));
		
		Message notifyMsg = new Message(MessageType.NOTIFY, this.id, getFirtSuccesor());
		this.router.send(notifyMsg);
	}

	public void onNotify(Message message) {
		Integer nPrime = message.getSourceNode();
		
		if (this.predecessor == null || insideInterval(nPrime, this.predecessor, this.id)) {
			this.predecessor = nPrime;
			
			if (this.state == NodeState.NEW) {
				this.state = NodeState.SUBSCRIBED;
			}
		}
	}
	
	
	/**stabilize and ack-stabilize require at most 4 ticks
	 * if the correct ack-stabilize is received, lastStabilizeId will be = null
	 * function called every 5 ticks, so no priority needed, safe because 5>4 and
	 * 5<6 that is the tick in which lastStabilizeId is set again
	 * */
	public void stabilizeFailCheck() {
		
		if(this.lastStabilizeId != null) {
			removeFirtSuccessor();
		}
	}
	
	private void MergeSuccessorList(ArrayList<Integer> msgSuccessorList) {
		if (this.successorList.size() >= msgSuccessorList.size()) {
			for (int i = 0; i < msgSuccessorList.size() - 1; i++) {
				this.successorList.set(1 + i, msgSuccessorList.get(i));
			}
		}
		else{
			for (int i = 0; i < msgSuccessorList.size(); i++) {
				this.successorList.add(1 + i, msgSuccessorList.get(i));
			}
		}
		
		while(this.successorList.size() > SUCCESSOR_TABLE_SIZE) {
			this.successorList.remove( this.successorList.size() - 1 );
		}
	}
	
	public Integer getFirtSuccesor() {
		//avoid throwing exception
		if(this.successorList.size() == 0) {
			return null;
		}
		else {
			return this.successorList.get(0);
		}
	}
	
	
	public void removeFirtSuccessor() {
		if(this.successorList.size() != 0) {
			this.successorList.remove(0);
		}
	}
	
	// --------------- getter and setter methods ---------------
	
	public Integer[] getFingerTable() {
		return fingerTable;
	}

	public void setFingerTable(Integer[] fingerTable) {
		this.fingerTable = fingerTable;
	}

	public void setSuccessorList(ArrayList<Integer> successorList) {
		for(int i=0; i < successorList.size(); i++) {
			this.successorList.add(successorList.get(i));
		}
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

	public NodeState getState() {
		return state;
	}

	public void setState(NodeState state) {
		this.state = state;
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
}
