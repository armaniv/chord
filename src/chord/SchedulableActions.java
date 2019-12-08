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
			Node node = this.node;
			node.receive(message);
		}
	}
}