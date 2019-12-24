package chord;

import java.awt.Color;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import saf.v3d.scene.VSpatial;

public class NodeStyle2D extends DefaultStyleOGL2D {

	@Override
	public Color getColor(Object o) {
		Node node = (Node) o;

		if (node.getState() == NodeState.NEW) {
			return Color.LIGHT_GRAY;
		} else
			return Color.BLACK;
	}

	public VSpatial getVSpatial(Object agent, VSpatial spatial) {
		return shapeFactory.createCircle(4, 16);
	}
}
