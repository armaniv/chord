package chord;

import java.util.HashMap;

import chord.SchedulableActions.ReceiveMessage;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;

public class Router {
	private HashMap<Integer, Node> nodes;

	public Router() {
		this.nodes = new HashMap<Integer, Node>();
	}

	public void setNodes(HashMap<Integer, Node> nodes) {
		for (Integer key : nodes.keySet()) {
			Node node = nodes.get(key);
			this.nodes.put(key, node);
		}
	}

	public void send(Message message) {
		Node receiver = nodes.get(message.getDestinationNode());
		
		if (receiver != null) {
			ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
			ScheduleParameters scheduleParameters = ScheduleParameters
					.createOneTime(schedule.getTickCount() + randomDelay(), PriorityType.RANDOM);
			schedule.schedule(scheduleParameters, new ReceiveMessage(receiver, message));
		}
	}

	public int randomDelay() {
		return RandomHelper.nextIntFromTo(1, 2);
	}

	public void removeANode(Integer nodeID) {
		this.nodes.remove(nodeID);
	}
}
