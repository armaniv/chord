package chord;

import repast.simphony.engine.schedule.IAction;

public class SchedulableActions {

	public static class ReceiveMessage implements IAction {
		private Node node;
		private Message message;

		public ReceiveMessage(Node node, Message message) {
			this.message = message;
			this.node = node;
		}

		public void execute() {
			try {
				this.node.receive(message);
			} catch (Exception e) {
			}
		}
	}

	public static class FailCheck implements IAction {
		private Node node;
		private Integer reqId;
		private Integer nodeIdToCheck;
		private boolean isKnown;

		public FailCheck(Node node, Integer reqId, Integer nodeIdToCheck, boolean isKnown) {
			this.reqId = reqId;
			this.node = node;
			this.nodeIdToCheck = nodeIdToCheck;
			this.isKnown = isKnown;
		}

		public void execute() {
			this.node.failCheck(reqId, nodeIdToCheck, isKnown);
		}
	}

	public static class MasterRetryLookup implements IAction {
		private ChordNode master;
		private Integer key;
		private Integer nodeId;

		public MasterRetryLookup(ChordNode master, Integer key, Integer nodeId) {
			this.key = key;
			this.master = master;
		}

		public void execute() {
			this.master.retryLookup(nodeId, key);
		}
	}

	public static class StabilizeFailCheck implements IAction {
		private Node node;

		public StabilizeFailCheck(Node node) {
			this.node = node;
		}

		public void execute() {
			this.node.stabilizeFailCheck();
		}
	}
}