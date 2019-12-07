package chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;

public class ChordNode {

	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Integer SPACEDIMENSION = 64;
	private Integer FINGER_TABLE_SIZE = (int) (Math.log(SPACEDIMENSION) / Math.log(2));

	private Integer num_total_nodes;
	HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

	public ChordNode(Context<Object> context, ContinuousSpace<Object> space, int num_init_nodes, int num_total_nodes) {
		this.context = context;
		this.space = space;
		this.num_total_nodes = num_total_nodes;

		createInitialNetwork(num_init_nodes);
	}

	private void createInitialNetwork(int num_init_nodes) {
		Random rnd = new Random(); // Java random, approximately uniform distributed

		for (int i = 0; i < num_init_nodes; i++) {
			int id = rnd.nextInt(SPACEDIMENSION);

			while (nodes.containsKey(id)) {
				id = rnd.nextInt(SPACEDIMENSION);
			}

			Node node = new Node(id, FINGER_TABLE_SIZE);
			nodes.put(id, node);
			context.add(node);
			visualizeNode(node);
		}

		createInitialFingerTables();
	}

	private void createInitialFingerTables() {
		List<Integer> sortedKeys = new ArrayList<>(nodes.keySet());
		Collections.sort(sortedKeys);
		// System.out.println(Arrays.toString(sortedKeys.toArray()));

		for (Integer key : nodes.keySet()) {
			Node node = nodes.get(key);
			int id = node.getId();
			Integer[] fingerTable = new Integer[FINGER_TABLE_SIZE];

			for (int i = 0; i < FINGER_TABLE_SIZE; i++) {
				int value = id + (int) Math.pow(2, i);

				for (int j = 0; j < sortedKeys.size(); j++) {
					if (sortedKeys.get(j) >= (value % SPACEDIMENSION)) {
						fingerTable[i] = sortedKeys.get(j);
						break;
					}
				}
				if (fingerTable[i] == null) {
					fingerTable[i] = sortedKeys.get(0);
				}
			}

			// System.out.println(id + ": " + Arrays.toString(fingerTable));
			node.setFingerTable(fingerTable);
		}
	}

	private void visualizeNode(Node node) {
		double spaceSize = space.getDimensions().getHeight();
		double center = spaceSize / 2;
		double radius = center / 2;

		double theta = 2 * Math.PI * node.getId() / SPACEDIMENSION;
		double x = center + radius * Math.cos(theta);
		double y = center + radius * Math.sin(theta);
		space.moveTo(node, x, y);
	}
}
