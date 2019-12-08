package chord;

import repast.simphony.engine.schedule.IAction;

public class SchedulableActions {

	public static class ReceiveMessage implements IAction {
		private Integer sourceNodeId;
		private Integer destinationNodeId;
		private Message message;
		private Router router;

		public ReceiveMessage(Integer sourceNodeId, Integer destinationNodeId, Message message, Router router) {
			this.sourceNodeId = sourceNodeId;
			this.destinationNodeId = destinationNodeId;
			this.message = message;
			this.router = router;
		}

		public void execute() {
			this.router.send(message, sourceNodeId, destinationNodeId);
		}
	}
}