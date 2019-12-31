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
	private int maxRetry = 5;
	private Integer counter = 0;
	private Integer lastStabilizeId = -1;

	public Node(Integer id, Integer FINGER_TABLE_SIZE, Integer SUCCESSOR_TABLE_SIZE, Router router,
			ChordNode masterNode, NodeState state) {
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
			this.onFindSucc(message);
			break;
		case FOUND_KEY:
			this.onFoundKey(message);
			break;
		case FOUND_SUCC:
			this.onFoundSucc(message);
			break;
		case STABILIZE:
			this.onStabilize(message);
			break;
		case ACK_STABILIZE:
			this.onACKStabilize(message);
			break;
		case NOTIFY:
			this.onNotify(message);
			break;
		case NOTIFY_SUCC_CHANGE:
			this.onNotifySuccChange(message);
			break;
		case NOTIFY_CRASHED_NODE:
			this.onNotifyCrashedNode(message);
			break;
		default:
			break;
		}
	}

	public void onFindSucc(Message message) {
		if (insideInterval(message.getKey(), this.predecessor, this.id + 1)) {
			Message ack = new Message(MessageType.FOUND_KEY, this.id, message.getSourceNode());
			ack.setKey(message.getKey());
			ack.setSubType(message.getSubType());
			ack.setSuccessor(this.id);
			ack.setReqId(message.getReqId());
			if (message.getSubType() == MessageType.JOIN) {
				ack.setSuccessorList(this.successorList);
				Integer oldPred = this.predecessor;
				this.predecessor = message.getSourceNode();
				notifySuccChange(this.predecessor, oldPred);
			}
			router.send(ack);
		} else {
			Integer successor = findSuccessor(message.getKey());
			Message ack = new Message(MessageType.FOUND_SUCC, this.id, message.getSourceNode());
			ack.setKey(message.getKey());
			ack.setSubType(message.getSubType());
			ack.setSuccessor(successor);
			ack.setReqId(message.getReqId());
			this.router.send(ack);
		}
	}

	public Integer findSuccessor(Integer id) {
		if (insideInterval(id, this.id, getFirstSuccesor() + 1)) {
			return getFirstSuccesor();
		} else {
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
		return this.id;
	}

	private void notifySuccChange(Integer succ, Integer oldPred) {
		Message msg = new Message(MessageType.NOTIFY_SUCC_CHANGE, this.id, oldPred);
		msg.setSuccessor(succ);
		msg.setSuccessorList(this.successorList);
		this.router.send(msg);
	}

	private void onNotifySuccChange(Message message) {
		this.successorList.add(0, message.getSuccessor());
		MergeSuccessorList(message.getSuccessorList(), true);

		// notify new succesor
		Message notifyMsg = new Message(MessageType.NOTIFY, this.id, getFirstSuccesor());
		this.router.send(notifyMsg);
	}

	private ArrayList<Integer> getFullTable() {
		HashSet<Integer> hs = new HashSet<Integer>();
		hs.addAll(this.successorList);

		for (int i = this.fingerTable.length - 1; i >= 0; i--) {
			if (fingerTable[i] == null)
				continue;
			hs.add(fingerTable[i]);
		}

		ArrayList<Integer> fullTable = new ArrayList<>();
		fullTable.addAll(hs);

		fullTable.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer arg0, Integer arg1) {
				int dist1 = distance(id, arg0);
				int dist2 = distance(id, arg1);
				return dist1 - dist2;
			}
		});

		return fullTable;
	}

	public int distance(Integer a, Integer b) {
		if (b >= a)
			return b - a;
		return b + (int) Math.pow(2, this.FINGER_TABLE_SIZE) - a;

	}

	// forward the request
	// schedule timeout check
	public void onFoundSucc(Message message) {
		Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, message.getSuccessor());
		findSuccMsg.setSubType(message.getSubType());
		findSuccMsg.setKey(message.getKey());
		findSuccMsg.setReqId(message.getReqId());
		sendFindSucc(findSuccMsg, false);
		if (findSuccMsg.getSubType().equals(MessageType.LOOKUP)) {
			this.masterNode.removeAnEdge(this.id, message.getSourceNode());
		}
	}

	// remove pendingLookup and deliver it to master
	public void onFoundKey(Message message) {
		FindSuccReq request = this.pendingFindSuccReq.removeRequest(message.getReqId());
		ArrayList<Integer> messagePath = request.getMessagePath();
		switch (message.getSubType()) {
		case LOOKUP:
			this.masterNode.signalSuccessuful(request);
			this.masterNode.removeAnEdge(this.id, message.getSourceNode());
			break;
		case JOIN:
			ArrayList<Integer> succList = message.getSuccessorList();
			this.successorList.add(0, message.getSourceNode());
			MergeSuccessorList(succList, false);
			break;
		case FIX_FINGERS:
			Integer succ = message.getSuccessor();
			Integer next = request.getNext();
			Integer old = this.fingerTable[next];
			this.fingerTable[next] = succ;
			break;
		default:
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
		if (insideInterval(findSuccKey, this.predecessor, this.id + 1)) {
			FindSuccReq findSuccReq = new FindSuccReq(findSuccKey, this.counter++);
			findSuccReq.addNodeToPath(this.id);
			this.masterNode.signalSuccessuful(findSuccReq);
		} else {
			Integer knownSucc = findSuccessor(findSuccKey);
			Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, knownSucc);
			findSuccMsg.setSubType(MessageType.LOOKUP);
			findSuccMsg.setKey(findSuccKey);
			sendFindSucc(findSuccMsg, true);
		}
	}

	public void sendFindSucc(Message findSuccMsg, boolean isKnown) {
		FindSuccReq findSuccReq = this.pendingFindSuccReq.getRequest(findSuccMsg.getReqId());
		if (findSuccReq == null) {
			findSuccReq = new FindSuccReq(findSuccMsg.getKey(), this.counter++);
			findSuccReq.addNodeToPath(this.id);
		}
		Integer destNodeId = findSuccMsg.getDestinationNode();
		findSuccReq.addNodeToPath(destNodeId);
		findSuccReq.setType(findSuccMsg.getSubType());
		findSuccMsg.setReqId(findSuccReq.getId());
		if (findSuccMsg.getSubType().equals(MessageType.FIX_FINGERS)) {
			findSuccReq.setNext(this.next);
		}
		this.pendingFindSuccReq.addRequest(findSuccReq);
		if (isKnown) {
			scheduleFailCheck(findSuccReq, destNodeId, true);
		} else {
			scheduleFailCheck(findSuccReq, destNodeId, false);
		}

		this.router.send(findSuccMsg);

		if (findSuccMsg.getSubType().equals(MessageType.LOOKUP)) {
			this.masterNode.visualizeAnEdge(this.id, destNodeId);
		}
	}

	private void scheduleFailCheck(FindSuccReq request, Integer destinationNodeId, boolean isKnown) {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters scheduleParameters = ScheduleParameters.createOneTime(schedule.getTickCount() + 5,
				PriorityType.RANDOM);
		this.actions.add(schedule.schedule(scheduleParameters,
				new FailCheck(this, request.getId(), destinationNodeId, isKnown)));
	}

	public void removeAllSchedule() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		for (int i = 0; i < this.actions.size(); i++) {
			schedule.removeAction(this.actions.get(i));
		}
	}

	public void failCheck(Integer reqId, Integer nodeIdToCheck, boolean isKnown) {
		if (this.pendingFindSuccReq.isPathBroken(nodeIdToCheck, reqId)) {
			FindSuccReq unsuccessfulReq = this.pendingFindSuccReq.getRequest(reqId);

			switch (unsuccessfulReq.getType()) {
			case JOIN:
				this.pendingFindSuccReq.removeRequest(reqId);
				this.masterNode.signalUnseccessfulJoin(this);
				break;
			case LOOKUP:
				this.notifyCrashedNode(
						unsuccessfulReq.getMessagePath().get(unsuccessfulReq.getMessagePath().size() - 2),
						nodeIdToCheck);
				int toDoRetries = unsuccessfulReq.getMaxRetry();
				int alreadyDoneRetries = this.maxRetry - toDoRetries;
				if (isKnown)
					removeKnownNodeId(nodeIdToCheck);
				if (alreadyDoneRetries < this.maxRetry) {
					retryLookup(unsuccessfulReq);
				} else {
					this.pendingFindSuccReq.removeRequest(reqId);
					this.masterNode.signalUnsuccessful(unsuccessfulReq, this.id);
				}
				break;
			case FIX_FINGERS:
				if (isKnown)
					removeKnownNodeId(nodeIdToCheck);
				break;
			default:
				break;
			}
		}
	}

	private void removeKnownNodeId(Integer nodeId) {
		for (int i = 0; i < this.successorList.size(); i++) {
			if (this.successorList.get(i).equals(nodeId)) {
				this.successorList.remove(i);
				break;
			}
		}
		for (int i = 0; i < this.fingerTable.length; i++) {
			if (this.fingerTable[i] != null) {
				if (this.fingerTable[i].equals(nodeId)) {
					this.fingerTable[i] = null;
				}
			}
		}
	}

	private void retryLookup(FindSuccReq lookupReq) {
		int findSuccKey = lookupReq.getFindSuccKey();
		lookupReq.prepareForRetry();
		this.pendingFindSuccReq.updateRequest(lookupReq);
		Integer successor = findSuccessor(findSuccKey);
		Message findSuccMsg = new Message(MessageType.FIND_SUCC, this.id, successor);
		findSuccMsg.setSubType(MessageType.LOOKUP);
		findSuccMsg.setKey(findSuccKey);
		findSuccMsg.setReqId(lookupReq.getId());
		sendFindSucc(findSuccMsg, true);
	}

	public void join(Integer nodeId) {
		this.predecessor = null;
		Message joinMessage = new Message(MessageType.FIND_SUCC, this.id, nodeId);
		joinMessage.setKey(this.id);
		joinMessage.setSubType(MessageType.JOIN);
		sendFindSucc(joinMessage, false);
	}

	@ScheduledMethod(start = 4, interval = 600)
	public void fixFingers() {
		if (getFirstSuccesor() == null)
			return; // still JOINing
		this.next++;
		if (this.next >= this.fingerTable.length) {
			this.next = 1;
		}
		Integer key = (this.id + (int) Math.pow(2, next - 1)) % (int) Math.pow(2, fingerTable.length);
		Integer succ = findSuccessor(key);
		Message fixFingersMessage = new Message(MessageType.FIND_SUCC, this.id, succ);
		fixFingersMessage.setSubType(MessageType.FIX_FINGERS);
		fixFingersMessage.setKey(key);
		sendFindSucc(fixFingersMessage, true);
	}

	public void retryFixFingers(int oldNext) {
		if (getFirstSuccesor() == null)
			return; // still JOINing
		Integer key = (this.id + (int) Math.pow(2, oldNext - 1)) % (int) Math.pow(2, fingerTable.length);
		Integer succ = findSuccessor(key);
		Message fixFingersMessage = new Message(MessageType.FIND_SUCC, this.id, succ);
		fixFingersMessage.setSubType(MessageType.FIX_FINGERS);
		fixFingersMessage.setKey(key);
		sendFindSucc(fixFingersMessage, true);
	}

	@ScheduledMethod(start = 4, interval = 600)
	public void stabilize() {
		if (getFirstSuccesor() != null) {
			Message msgStabilize = new Message(MessageType.STABILIZE, this.id, getFirstSuccesor());
			this.lastStabilizeId = this.counter++;
			msgStabilize.setReqId(lastStabilizeId);
			this.router.send(msgStabilize);

			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			ScheduleParameters scheduleParameters = ScheduleParameters.createOneTime(schedule.getTickCount() + 5,
					PriorityType.RANDOM);
			this.actions.add(schedule.schedule(scheduleParameters, new StabilizeFailCheck(this)));
		}
	}

	public void onStabilize(Message message) {
		Message replayStabilize = new Message(MessageType.ACK_STABILIZE, this.id, message.getSourceNode());
		replayStabilize.setReqId(message.getReqId());
		if (this.predecessor != null) {
			replayStabilize.setPredecessor(this.predecessor);
		}
		replayStabilize.setSuccessorList(this.successorList);
		this.router.send(replayStabilize);
	}

	public void onACKStabilize(Message message) {
		Integer x = message.getPredecessor();
		Integer ACKId = message.getReqId();

		if (this.lastStabilizeId.equals(ACKId)) {
			this.lastStabilizeId = null;
		}

		if (insideInterval(x, this.id, getFirstSuccesor())) {
			this.successorList.add(0, x);
			MergeSuccessorList(message.getSuccessorList(), true);
		} else {
			MergeSuccessorList(message.getSuccessorList(), false);
		}

		Message notifyMsg = new Message(MessageType.NOTIFY, this.id, getFirstSuccesor());
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

	public void notifyCrashedNode(Integer destNodeId, Integer crashedNodeId) {
		Message message = new Message(MessageType.NOTIFY_CRASHED_NODE, this.id, destNodeId);
		message.setSuccessor(crashedNodeId);
		this.router.send(message);
	}

	public void onNotifyCrashedNode(Message message) {
		this.removeKnownNodeId(message.getSuccessor());
	}

	/**
	 * stabilize and ack-stabilize require at most 4 ticks if the correct
	 * ack-stabilize is received, lastStabilizeId will be = null function called
	 * every 5 ticks, so no priority needed, safe because 5>4 and 5<6 that is the
	 * tick in which lastStabilizeId is set again
	 */
	public void stabilizeFailCheck() {

		if (this.lastStabilizeId != null) {
			removeFirtSuccessor();
		}
	}

	private void MergeSuccessorList(ArrayList<Integer> msgSuccessorList, boolean newSuccesor) {
		Integer elemZero = this.successorList.get(0);
		Integer elemOne = null;

		if (newSuccesor) {
			elemOne = this.successorList.get(1);
		}

		this.successorList = makeDeepCopyInteger(msgSuccessorList);

		if (newSuccesor && elemOne != null) {
			this.successorList.add(0, elemOne);
		}

		this.successorList.add(0, elemZero);

		while (this.successorList.size() > SUCCESSOR_TABLE_SIZE) {
			this.successorList.remove(this.successorList.size() - 1);
		}
	}

	private ArrayList<Integer> makeDeepCopyInteger(ArrayList<Integer> old) {
		ArrayList<Integer> copy = new ArrayList<Integer>(old.size());
		for (Integer i : old) {
			copy.add(i);
		}
		return copy;
	}

	public Integer getFirstSuccesor() {
		// avoid throwing exception
		if (this.successorList.size() == 0) {
			return null;
		} else {
			return this.successorList.get(0);
		}
	}

	public void removeFirtSuccessor() {
		if (this.successorList.size() != 0) {
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
		for (int i = 0; i < successorList.size(); i++) {
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
