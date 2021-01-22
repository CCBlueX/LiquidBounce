package net.ccbluex.liquidbounce.features.module.modules.player;

import static org.lwjgl.opengl.GL11.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.ccbluex.liquidbounce.api.enums.BlockType;
import net.ccbluex.liquidbounce.api.enums.MaterialType;
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerBlockPlacement;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging.WAction;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity;
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing;
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.ImmutableVec3;
import net.ccbluex.liquidbounce.utils.RaycastUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;

@ModuleInfo(name = "ExtendedReach", description = "Upgraded combat and block reach over 100+ blocks.", category = ModuleCategory.PLAYER)
public class ExtendedReach extends Module
{
	private List<ImmutableVec3> path = new ArrayList<>();
	public final FloatValue combatReach = new FloatValue("CombatReach", 100, 6, 128);
	public final FloatValue buildReach = new FloatValue("BuildReach", 100, 6, 128);
	private final BoolValue pathEspValue = new BoolValue("PathESP", true);
	private final IntegerValue pathEspTimeValue = new IntegerValue("PathESPTime", 1000, 100, 3000);
	private final IntegerValue maxDashDistanceValue = new IntegerValue("DashDistance", 5, 1, 10);
	private final MSTimer pathESPTimer = new MSTimer();

	@Override
	public final void onEnable()
	{
		path.clear();
	}

	@Override
	public final void onDisable()
	{
		path.clear();
	}

	@EventTarget
	public final void onRender3D(final Render3DEvent event)
	{
		if (!path.isEmpty() && !pathESPTimer.hasTimePassed(pathEspTimeValue.get()) && pathEspValue.get())
			for (final ImmutableVec3 vec : path)
				drawPath(vec);
	}

	@EventTarget
	public final void onPacket(final PacketEvent ep)
	{
		if (mc.getThePlayer() == null)
			return;

		final IPacket p = ep.getPacket();

		if (classProvider.isCPacketPlayerBlockPlacement(p))
		{
			final ICPacketPlayerBlockPlacement packet = p.asCPacketPlayerBlockPlacement();
			final WBlockPos pos = packet.getPosition();
			final IItemStack stack = packet.getStack();
			final double dist = Math.sqrt(mc.getThePlayer().getDistanceSq(pos));
			if (dist > 6 && pos.getY() != -1 && (stack != null || classProvider.isBlockContainer(BlockUtils.getState(pos).getBlock())))
			{
				final ImmutableVec3 from = new ImmutableVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY(), mc.getThePlayer().getPosZ());
				final ImmutableVec3 to = new ImmutableVec3(pos.getX(), pos.getY(), pos.getZ());
				path = computePath(from, to);

				// Travel to the target block.
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));

				pathESPTimer.reset();
				mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(p);

				// Go back to the home.
				Collections.reverse(path);
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));

				ep.cancelEvent();
			}
		}

		if (classProvider.isCPacketPlayerDigging(p))
		{
			final ICPacketPlayerDigging packet = p.asCPacketPlayerDigging();
			final WAction act = packet.getStatus();
			final WBlockPos pos = packet.getPosition();
			final IEnumFacing face = packet.getFacing();
			final double distance = Math.sqrt(mc.getThePlayer().getDistanceSq(pos));
			if (distance > 6 && act == WAction.START_DESTROY_BLOCK)
			{
				final ImmutableVec3 topFrom = new ImmutableVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY(), mc.getThePlayer().getPosZ());
				final ImmutableVec3 to = new ImmutableVec3(pos.getX(), pos.getY(), pos.getZ());
				path = computePath(topFrom, to);

				// Travel to the target.
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));

				pathESPTimer.reset();
				final IPacket end = classProvider.createCPacketPlayerDigging(WAction.STOP_DESTROY_BLOCK, pos, face);
				mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(p);
				mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(end);

				// Go back to the home.
				Collections.reverse(path);
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));
				ep.cancelEvent();
			}
			else if (act == WAction.ABORT_DESTROY_BLOCK)
				ep.cancelEvent();
		}
	}

	@EventTarget
	public final void onMotion(final MotionEvent event)
	{
		if (event.getEventState() == EventState.PRE)
		{
			final IEntity facedEntity = RaycastUtils.raycastEntity(combatReach.get(), classProvider::isEntityLivingBase);
			IEntityLivingBase targetEntity = null;
			if (mc.getGameSettings().getKeyBindAttack().isKeyDown() && EntityUtils.isSelected(facedEntity, true) && Objects.requireNonNull(mc.getThePlayer()).getDistanceSqToEntity(facedEntity) >= 1)
				targetEntity = facedEntity.asEntityLivingBase();

			if (targetEntity != null)
			{
				final ImmutableVec3 from = new ImmutableVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY(), mc.getThePlayer().getPosZ());
				final ImmutableVec3 to = new ImmutableVec3(targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ());
				path = computePath(from, to);

				// Travel to the target entity.
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));

				pathESPTimer.reset();

				mc.getThePlayer().swingItem();
				mc.getThePlayer().getSendQueue().addToSendQueue(classProvider.createCPacketUseEntity(targetEntity, ICPacketUseEntity.WAction.ATTACK));
				mc.getThePlayer().onCriticalHit(targetEntity);

				// Go back to the home.
				Collections.reverse(path);
				for (final ImmutableVec3 pathElm : path)
					mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));
			}
		}
	}

	private List<ImmutableVec3> computePath(ImmutableVec3 topFrom, final ImmutableVec3 to)
	{
		if (!canPassThrow(new WBlockPos(topFrom.getX(), topFrom.getY(), topFrom.getZ())))
			topFrom = topFrom.addVector(0, 1, 0);

		final PathFinder pathfinder = new PathFinder(topFrom, to);
		pathfinder.compute();

		int i = 0;
		ImmutableVec3 lastLoc = null;
		ImmutableVec3 lastDashLoc = null;
		final List<ImmutableVec3> path = new ArrayList<>();
		final List<ImmutableVec3> pathFinderPath = pathfinder.getPath();
		for (final ImmutableVec3 pathElm : pathFinderPath)
		{
			if (i == 0 || i == pathFinderPath.size() - 1)
			{
				if (lastLoc != null)
					path.add(lastLoc.addVector(0.5, 0, 0.5));
				path.add(pathElm.addVector(0.5, 0, 0.5));
				lastDashLoc = pathElm;
			}
			else
			{
				boolean canContinue = true;
				final float maxDashDistance = maxDashDistanceValue.get();
				if (pathElm.squareDistanceTo(lastDashLoc) > maxDashDistance * maxDashDistance)
					canContinue = false;
				else
				{
					final double minX = Math.min(lastDashLoc.getX(), pathElm.getX());
					final double minY = Math.min(lastDashLoc.getY(), pathElm.getY());
					final double minZ = Math.min(lastDashLoc.getZ(), pathElm.getZ());
					final double maxX = Math.max(lastDashLoc.getX(), pathElm.getX());
					final double maxY = Math.max(lastDashLoc.getY(), pathElm.getY());
					final double maxZ = Math.max(lastDashLoc.getZ(), pathElm.getZ());
					cordsLoop:
					for (int x = (int) minX; x <= maxX; x++)
						for (int y = (int) minY; y <= maxY; y++)
							for (int z = (int) minZ; z <= maxZ; z++)
								if (!PathFinder.checkPositionValidity(x, y, z, false))
								{
									canContinue = false;
									break cordsLoop;
								}
				}
				if (!canContinue)
				{
					path.add(lastLoc.addVector(0.5, 0, 0.5));
					lastDashLoc = lastLoc;
				}
			}
			lastLoc = pathElm;
			i++;
		}
		return path;
	}

	private boolean canPassThrow(final WBlockPos pos)
	{
		final IIBlockState state = BlockUtils.getState(new WBlockPos(pos.getX(), pos.getY(), pos.getZ()));
		final IBlock block = state.getBlock();
		return classProvider.getMaterialEnum(MaterialType.AIR).equals(block.getMaterial(state)) || classProvider.getMaterialEnum(MaterialType.PLANTS).equals(block.getMaterial(state)) || classProvider.getMaterialEnum(MaterialType.VINE).equals(block.getMaterial(state)) || classProvider.getBlockEnum(BlockType.LADDER).equals(block) || classProvider.getBlockEnum(BlockType.WATER).equals(block) || classProvider.getBlockEnum(BlockType.FLOWING_WATER).equals(block) || classProvider.getBlockEnum(BlockType.WALL_SIGN).equals(block) || classProvider.getBlockEnum(BlockType.STANDING_SIGN).equals(block);
	}

	public final void drawPath(final ImmutableVec3 vec)
	{
		final double x = vec.getX() - mc.getRenderManager().getRenderPosX();
		final double y = vec.getY() - mc.getRenderManager().getRenderPosY();
		final double z = vec.getZ() - mc.getRenderManager().getRenderPosZ();
		final double width = 0.3;
		final double height = mc.getThePlayer().getEyeHeight();

		// pre3D
		glPushMatrix();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glShadeModel(GL_SMOOTH);
		glDisable(GL_TEXTURE_2D);
		glEnable(GL_LINE_SMOOTH);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_LIGHTING);
		glDepthMask(false);
		glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

		glLoadIdentity();
		mc.getEntityRenderer().setupCameraTransform(mc.getTimer().getRenderPartialTicks(), 2);
		final Color[] colors =
		{
				Color.black, Color.white
		};
		for (int i = 0; i < 2; i++)
		{
			RenderUtils.glColor(colors[i]);
			glLineWidth(3 - i * 2);
			glBegin(GL_LINE_STRIP);
			glVertex3d(x - width, y, z - width);
			glVertex3d(x - width, y, z - width);
			glVertex3d(x - width, y + height, z - width);
			glVertex3d(x + width, y + height, z - width);
			glVertex3d(x + width, y, z - width);
			glVertex3d(x - width, y, z - width);
			glVertex3d(x - width, y, z + width);
			glEnd();
			glBegin(GL_LINE_STRIP);
			glVertex3d(x + width, y, z + width);
			glVertex3d(x + width, y + height, z + width);
			glVertex3d(x - width, y + height, z + width);
			glVertex3d(x - width, y, z + width);
			glVertex3d(x + width, y, z + width);
			glVertex3d(x + width, y, z - width);
			glEnd();
			glBegin(GL_LINE_STRIP);
			glVertex3d(x + width, y + height, z + width);
			glVertex3d(x + width, y + height, z - width);
			glEnd();
			glBegin(GL_LINE_STRIP);
			glVertex3d(x - width, y + height, z + width);
			glVertex3d(x - width, y + height, z - width);
			glEnd();
		}

		// post3D
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_LINE_SMOOTH);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glPopMatrix();
		glColor4f(1, 1, 1, 1);
	}

	@Override
	public final String getTag()
	{
		return String.valueOf(maxDashDistanceValue.get());
	}
}
