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
		ArrayList<AstarNode> openList = new ArrayList<>();
		ArrayList<AstarNode> closedList = new ArrayList<>();

		MSTimer timer = new MSTimer();
		timer.reset();

		openList.add(begin);

		AstarNode currentNode = null;
		do
		{
			if (openList.size() == 0) break;

			currentNode = Collections.min(openList,Comparator.comparing(x -> x.f(begin,end)));

			closedList.add(currentNode);
			openList.remove(currentNode);

			ArrayList<AstarNode> neighbors = currentNode.neighbors();

			for (AstarNode element: neighbors)
			{
				if (closedList.contains(element))
					continue;

				if (!openList.contains(element))
					openList.add(element);
				else
				{
					element = openList.get(openList.indexOf(element));

					AstarNode parent = element.parent();
					double g = parent.g(begin) + element.costToParent();
					double tentativeG = element.g(begin);

					if (tentativeG < g)
						element.setParent(currentNode);
				}
			}

			if (openList.size() > 10000) return new ArrayList<>();
			if (timer.hasTimePassed(timeout_ms)) return new ArrayList<>();
		} while (!op.shouldEnd(currentNode,end));

		ArrayList<AstarNode> list = new ArrayList<>();

		while (currentNode.parent() != null)
		{
			list.add(0,currentNode);
			currentNode = currentNode.parent();
		}
		list.add(0,currentNode);

		return list;
	}
}
