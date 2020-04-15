/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.astar;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class Astar extends MinecraftInstance
{
	public interface AstarEndOperator
	{
		boolean shouldEnd(AstarNode current, AstarNode end);
	}

	public interface AstarNode
	{
		double f(AstarNode begin, AstarNode end);
		double g(AstarNode begin);
		double h(AstarNode end);

		AstarNode parent();
		double costToParent();
		void setParent(AstarNode p);

		ArrayList<AstarNode> neighbors();

		boolean equals(AstarNode p);
	}



	public static ArrayList<AstarNode> find_path(final AstarNode begin, final AstarNode end,
	                                      AstarEndOperator op,int timeout_ms)
	{
		ArrayList<AstarNode> openList = new ArrayList<AstarNode>();
		ArrayList<AstarNode> closedList = new ArrayList<AstarNode>();

		MSTimer timer = new MSTimer();
		timer.reset();

		openList.add(begin);

		AstarNode current_node = null;
		do
		{
			if (openList.size() == 0) break;

			current_node = Collections.min(openList,Comparator.comparing(x -> x.f(begin,end)));

			closedList.add(current_node);
			openList.remove(current_node);

			ArrayList<AstarNode> neibors = current_node.neighbors();

			for (AstarNode element: neibors)
			{
				if (closedList.contains(element))
					continue;

				if (!openList.contains(element))
					openList.add(element);
				else
				{
					element = openList.get(openList.indexOf(element));

					AstarNode parent = element.parent();
					double original_g = parent.g(begin) + element.costToParent();
					double new_g = element.g(begin);

					if (new_g < original_g)
						element.setParent(current_node);
				}
			}

			if (openList.size() > 10000) return new ArrayList<AstarNode>();
			if (timer.hasTimePassed(timeout_ms)) return new ArrayList<AstarNode>();
		} while (!op.shouldEnd(current_node,end));

		ArrayList<AstarNode> list = new ArrayList<>();

		while (current_node.parent() != null)
		{
			list.add(0,current_node);
			current_node = current_node.parent();
		}
		list.add(0,current_node);

		return list;
	}
}
