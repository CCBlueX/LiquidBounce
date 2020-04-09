package net.ccbluex.liquidbounce.utils.astar;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class Astar extends MinecraftInstance
{
	public static ArrayList<AstarNode> find_path(final AstarNode begin, final AstarNode end,
	                                      AstarEndOperator op,int timeout_ms)
	{
		ArrayList<AstarNode> openlist = new ArrayList<AstarNode>();
		ArrayList<AstarNode> closedlist = new ArrayList<AstarNode>();

		MSTimer timer = new MSTimer();
		timer.reset();

		openlist.add(begin);

		AstarNode current_node = null;
		do
		{
			if (openlist.size() == 0) break;

			current_node = Collections.min(openlist,Comparator.comparing(x -> x.cauculate_f(begin,end)));

			closedlist.add(current_node);
			openlist.remove(current_node);

			ArrayList<AstarNode> neibors = current_node.get_neighbors();

			for (AstarNode element: neibors)
			{
				if (closedlist.contains(element))
					continue;

				if (!openlist.contains(element))
					openlist.add(element);
				else
				{
					element = openlist.get(openlist.indexOf(element));

					AstarNode parent = element.get_parent();
					double original_g = parent.cauculate_g(begin) + element.cauculate_cost_to_parent();
					double new_g = element.cauculate_g(begin);

					if (new_g < original_g)
						element.set_parent(current_node);
				}
			}

			if (openlist.size() > 10000) return new ArrayList<AstarNode>();
			if (timer.hasTimePassed(timeout_ms)) return new ArrayList<AstarNode>();
		} while (!op.shouldend(current_node,end));

		ArrayList<AstarNode> list = new ArrayList<>();

		while (current_node.get_parent() != null)
		{
			list.add(0,current_node);
			current_node = current_node.get_parent();
		}
		list.add(0,current_node);

		return list;
	}
}
