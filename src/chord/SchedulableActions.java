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
			this.node.receive(message);
		}
	}
	
	public static class FailCheck implements IAction {
		private Node node;
		private Integer lookupKey;
		private Integer nodeIdToCheck;

		public FailCheck(Node node, Integer lookupKey, Integer nodeIdToCheck) {
			this.lookupKey = lookupKey;
			this.node = node;
			this.nodeIdToCheck = nodeIdToCheck;
		}

		public void execute() {
			this.node.failCheck(lookupKey, nodeIdToCheck);
		}
	}
}