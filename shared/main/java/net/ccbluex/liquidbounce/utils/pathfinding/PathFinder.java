package net.ccbluex.liquidbounce.utils.pathfinding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos;
import net.ccbluex.liquidbounce.utils.ImmutableVec3;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;

import org.jetbrains.annotations.Nullable;

public class PathFinder extends MinecraftInstance
{
	private static final ImmutableVec3[] flatCardinalDirections =
	{
			new ImmutableVec3(1, 0, 0), new ImmutableVec3(-1, 0, 0), new ImmutableVec3(0, 0, 1), new ImmutableVec3(0, 0, -1)
	};
	private final ImmutableVec3 startVec;
	private final ImmutableVec3 endVec;
	private final List<Hub> hubs = new ArrayList<>();
	private final ArrayList<Hub> hubsToWork = new ArrayList<>();
	private ArrayList<ImmutableVec3> path = new ArrayList<>();

	public PathFinder(final ImmutableVec3 startVec, final ImmutableVec3 endVec)
	{
		this.startVec = startVec.addVector(0, 0, 0).floor();
		this.endVec = endVec.addVector(0, 0, 0).floor();
	}

	public static boolean checkPositionValidity(final ImmutableVec3 loc, final boolean checkGround)
	{
		return checkPositionValidity((int) loc.getX(), (int) loc.getY(), (int) loc.getZ(), checkGround);
	}

	public static boolean checkPositionValidity(final int x, final int y, final int z, final boolean checkGround)
	{
		final WBlockPos block1 = new WBlockPos(x, y, z);
		final WBlockPos block2 = new WBlockPos(x, y + 1, z);
		final WBlockPos block3 = new WBlockPos(x, y - 1, z);
		return !isBlockSolid(block1) && !isBlockSolid(block2) && (isBlockSolid(block3) || !checkGround) && isSafeToWalkOn(block3);
	}

	private static boolean isBlockSolid(final WBlockPos blockpos)
	{
		final IIBlockState state = Objects.requireNonNull(BlockUtils.getState(blockpos));
		final IBlock block = state.getBlock();
		return Objects.requireNonNull(block.getMaterial(state)).blocksMovement() && block.isFullCube(state) || classProvider.isBlockSlab(block) || classProvider.isBlockStairs(block) || classProvider.isBlockCactus(block) || classProvider.isBlockChest(block) || classProvider.isBlockEnderChest(block) || classProvider.isBlockSkull(block) || classProvider.isBlockPane(block) || classProvider.isBlockFence(block) || classProvider.isBlockWall(block) || classProvider.isBlockGlass(block) || classProvider.isBlockPistonBase(block) || classProvider.isBlockPistonExtension(block) || classProvider.isBlockPistonMoving(block) || classProvider.isBlockStainedGlass(block) || classProvider.isBlockTrapDoor(block);
	}

	private static boolean isSafeToWalkOn(final WBlockPos blockpos)
	{
		final IBlock block = Objects.requireNonNull(BlockUtils.getState(blockpos)).getBlock();
		return !classProvider.isBlockFence(block) && !classProvider.isBlockWall(block);
	}

	public final ArrayList<ImmutableVec3> getPath()
	{
		return path;
	}

	public final void compute()
	{
		compute(1000, 4);
	}

	public final void compute(final int loops, final int depth)
	{
		path.clear();
		hubsToWork.clear();
		final ArrayList<ImmutableVec3> initPath = new ArrayList<>();
		initPath.add(startVec);
		hubsToWork.add(new Hub(startVec, null, initPath, startVec.squareDistanceTo(endVec), 0, 0));
		search:
		for (int i = 0; i < loops; i++)
		{
			hubsToWork.sort(new HubComparator());
			int j = 0;
			if (hubsToWork.isEmpty())
				break;
			for (final Hub hub : new ArrayList<>(hubsToWork))
			{
				j++;
				if (j > depth)
					break;
				hubsToWork.remove(hub);
				hubs.add(hub);

				for (final ImmutableVec3 direction : flatCardinalDirections)
				{
					final ImmutableVec3 loc = hub.getPosition().add(direction).floor();
					if (checkPositionValidity(loc, false) && addHub(hub, loc, 0))
						break search;
				}

				final ImmutableVec3 loc1 = hub.getPosition().addVector(0, 1, 0).floor();
				if (checkPositionValidity(loc1, false) && addHub(hub, loc1, 0))
					break search;

				final ImmutableVec3 loc2 = hub.getPosition().addVector(0, -1, 0).floor();
				if (checkPositionValidity(loc2, false) && addHub(hub, loc2, 0))
					break search;
			}
		}

		hubs.sort(new HubComparator());
		path = hubs.get(0).getPath();
	}

	@Nullable
	public final Hub isHubExisting(final ImmutableVec3 pos)
	{
		for (final Hub hub : hubs)
			if (hub.getPosition().getX() == pos.getX() && hub.getPosition().getY() == pos.getY() && hub.getPosition().getZ() == pos.getZ())
				return hub;

		return hubsToWork.stream().filter(hub -> hub.getPosition().getX() == pos.getX() && hub.getPosition().getY() == pos.getY() && hub.getPosition().getZ() == pos.getZ()).findFirst().orElse(null);

	}

	public final boolean addHub(final Hub parent, final ImmutableVec3 loc, final double cost)
	{
		final Hub existingHub = isHubExisting(loc);
		double totalCost = cost;
		if (parent != null)
			totalCost += parent.getTotalCost();

		if (existingHub == null)
		{
			final double minDistanceSquared = 9;
			if (loc.getX() == endVec.getX() && loc.getY() == endVec.getY() && loc.getZ() == endVec.getZ() || loc.squareDistanceTo(endVec) <= minDistanceSquared)
			{
				path.clear();
				path = parent.getPath();
				path.add(loc);
				return true;
			}
			final ArrayList<ImmutableVec3> path = new ArrayList<>(parent.getPath());
			path.add(loc);
			hubsToWork.add(new Hub(loc, parent, path, loc.squareDistanceTo(endVec), cost, totalCost));
		}
		else if (existingHub.getCost() > cost)
		{
			final ArrayList<ImmutableVec3> path = new ArrayList<>(parent.getPath());
			path.add(loc);
			existingHub.setPos(loc);
			existingHub.setPath(path);
			existingHub.setSquareDistanceToFromTarget(loc.squareDistanceTo(endVec));
			existingHub.setCost(cost);
			existingHub.setTotalCost(totalCost);
		}
		return false;
	}

	private static class Hub
	{
		private ImmutableVec3 pos;
		private ArrayList<ImmutableVec3> path;
		private double squareDistanceToFromTarget;
		private double cost;
		private double totalCost;

		public Hub(final ImmutableVec3 pos, final Hub parent, final ArrayList<ImmutableVec3> path, final double squareDistanceToFromTarget, final double cost, final double totalCost)
		{
			this.pos = pos;
			this.path = path;
			this.squareDistanceToFromTarget = squareDistanceToFromTarget;
			this.cost = cost;
			this.totalCost = totalCost;
		}

		public final ImmutableVec3 getPosition()
		{
			return pos;
		}

		public final void setPos(final ImmutableVec3 pos)
		{
			this.pos = pos;
		}

		public final ArrayList<ImmutableVec3> getPath()
		{
			return path;
		}

		public final void setPath(final ArrayList<ImmutableVec3> path)
		{
			this.path = path;
		}

		public final double getSquareDistanceToFromTarget()
		{
			return squareDistanceToFromTarget;
		}

		public final void setSquareDistanceToFromTarget(final double squareDistanceToFromTarget)
		{
			this.squareDistanceToFromTarget = squareDistanceToFromTarget;
		}

		public final double getCost()
		{
			return cost;
		}

		public final void setCost(final double cost)
		{
			this.cost = cost;
		}

		public final double getTotalCost()
		{
			return totalCost;
		}

		public final void setTotalCost(final double totalCost)
		{
			this.totalCost = totalCost;
		}
	}

	public static class HubComparator implements Comparator<Hub>, Serializable
	{
		private static final long serialVersionUID = -6706152803040254364L;

		@Override
		public final int compare(final Hub o1, final Hub o2)
		{
			return (int) (o1.getSquareDistanceToFromTarget() + o1.getTotalCost() - (o2.getSquareDistanceToFromTarget() + o2.getTotalCost()));
		}
	}
}
