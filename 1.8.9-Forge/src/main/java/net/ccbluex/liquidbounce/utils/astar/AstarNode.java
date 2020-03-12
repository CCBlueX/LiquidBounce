package net.ccbluex.liquidbounce.utils.astar;

import java.util.ArrayList;

public interface AstarNode
{
	public double cauculate_f(AstarNode begin,AstarNode end);

	public double cauculate_g(AstarNode begin);

	public AstarNode get_parent();

	public double cauculate_cost_to_parent();

	public ArrayList<AstarNode> get_neighbors();

	//public boolean equals(AstarNode p);

	public void set_parent(AstarNode p);

	double cauculate_h(AstarNode end);
}
