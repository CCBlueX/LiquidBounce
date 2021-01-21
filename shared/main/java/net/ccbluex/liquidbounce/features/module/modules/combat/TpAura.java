package net.ccbluex.liquidbounce.features.module.modules.combat;

import static org.lwjgl.opengl.GL11.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.enums.BlockType;
import net.ccbluex.liquidbounce.api.enums.EnumFacingType;
import net.ccbluex.liquidbounce.api.enums.MaterialType;
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState;
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging.WAction;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity;
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos;
import net.ccbluex.liquidbounce.event.AttackEvent;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.CPSCounter.MouseButton;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.ImmutableVec3;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.extensions.PlayerExtensionKt;
import net.ccbluex.liquidbounce.utils.pathfinding.PathFinder;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author LeakedPvP
 * @game   Minecraft
 */
@ModuleInfo(name = "TpAura", description = "InfiniteAura from Sigma 4.1.", category = ModuleCategory.COMBAT)
public class TpAura extends Module
{
	final IntegerValue maxCPS = new IntegerValue("MaxCPS", 6, 1, 20)
	{
		@Override
		public void onChanged(final Integer prevValue, final Integer newValue)
		{
			final int i = minCPS.get();
			if (i > newValue)
				set(i);
			attackDelay = TimeUtils.randomClickDelay(i, get());
		}
	};
	final IntegerValue minCPS = new IntegerValue("MinCPS", 6, 1, 20)
	{
		@Override
		public void onChanged(final Integer prevValue, final Integer newValue)
		{
			final int i = maxCPS.get();
			if (i < newValue)
				set(i);
			attackDelay = TimeUtils.randomClickDelay(get(), i);
		}
	};
	private final BoolValue pathEspValue = new BoolValue("PathESP", true);
	private final IntegerValue pathEspTime = new IntegerValue("PathESPTime", 1000, 100, 3000);
	private final FloatValue rangeValue = new FloatValue("Range", 30.0F, 6.1F, 1000.0F);
	private final IntegerValue hurtTimeValue = new IntegerValue("HurtTime", 10, 0, 10);
	private final IntegerValue maxTargetsValue = new IntegerValue("MaxTargets", 4, 1, 50);
	private final IntegerValue maxDashDistanceValue = new IntegerValue("DashDistance", 5, 1, 10);
	private final ListValue autoBlockValue = new ListValue("AutoBlock", new String[]
	{
			"Off", "Fake", "Packet", "AfterTick"
	}, "Packet");
	private final BoolValue swingValue = new BoolValue("Swing", true);

	// Attack Delay
	private final MSTimer attackTimer = new MSTimer();
	private long attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get());

	// Paths
	private final List<List<ImmutableVec3>> targetPaths = new ArrayList<>();

	// Targets
	public List<IEntityLivingBase> currentTargets = new CopyOnWriteArrayList<>();
	public IEntityLivingBase currentTarget;
	private List<ImmutableVec3> currentPath = new ArrayList<>();

	// Blocking Status
	public boolean clientSideBlockingStatus;
	private boolean serverSideBlockingStatus;

	@Override
	public final void onEnable()
	{
		currentTargets.clear();
	}

	@Override
	public final void onDisable()
	{
		currentTargets.clear();
		clientSideBlockingStatus = false;
	}

	@EventTarget
	public final void onUpdate(final UpdateEvent event)
	{
		if (mc.getThePlayer() == null)
			return;

		currentTargets = getTargets();

		if (attackTimer.hasTimePassed(attackDelay))
			if (!currentTargets.isEmpty())
			{
				targetPaths.clear();

				if (canBlock() && (mc.getThePlayer().isBlocking() || !"Off".equalsIgnoreCase(autoBlockValue.get())))
					clientSideBlockingStatus = true;

				for (int targetIndex = 0, targetCount = currentTargets.size() > maxTargetsValue.get() ? maxTargetsValue.get() : currentTargets.size(); targetIndex < targetCount; targetIndex++)
				{
					currentTarget = currentTargets.get(targetIndex);
					final ImmutableVec3 from = new ImmutableVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getPosY(), mc.getThePlayer().getPosZ());
					final ImmutableVec3 to = new ImmutableVec3(currentTarget.getPosX(), currentTarget.getPosY(), currentTarget.getPosZ());

					currentPath = computePath(from, to);
					targetPaths.add(currentPath); // Used for path esp

					// Unblock before attack
					if (mc.getThePlayer().isBlocking() || "Packet".equalsIgnoreCase(autoBlockValue.get()) || serverSideBlockingStatus)
					{
						mc.getNetHandler().addToSendQueue(classProvider.createCPacketPlayerDigging(WAction.RELEASE_USE_ITEM, WBlockPos.Companion.getORIGIN(), classProvider.getEnumFacing(EnumFacingType.DOWN)));
						serverSideBlockingStatus = false;
					}

					// Travel to the target
					for (final ImmutableVec3 path : currentPath)
						mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(path.getX(), path.getY(), path.getZ(), true));

					LiquidBounce.eventManager.callEvent(new AttackEvent(currentTarget));

					CPSCounter.registerClick(MouseButton.LEFT);

					if (swingValue.get())
						mc.getThePlayer().swingItem();

					// Attack target
					mc.getNetHandler().addToSendQueue(classProvider.createCPacketUseEntity(currentTarget, ICPacketUseEntity.WAction.ATTACK));

					// Block after attack
					if (canBlock() && !serverSideBlockingStatus && (mc.getThePlayer().isBlocking() || "Packet".equalsIgnoreCase(autoBlockValue.get())))
					{
						mc.getNetHandler().addToSendQueue(classProvider.createCPacketPlayerBlockPlacement(mc.getThePlayer().getInventory().getCurrentItemInHand()));
						serverSideBlockingStatus = true;
					}

					// Travel back to the original position
					Collections.reverse(currentPath);
					for (final ImmutableVec3 path : currentPath)
						mc.getNetHandler().getNetworkManager().sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(path.getX(), path.getY(), path.getZ(), true));
				}

				attackTimer.reset();
				attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get());
			}
			else
				clientSideBlockingStatus = false;
	}

	@EventTarget
	public final void onRender3D(final Render3DEvent event)
	{
		if (!currentPath.isEmpty() && pathEspValue.get())
		{
			for (final List<ImmutableVec3> targetPath : targetPaths)
				try
				{
					for (final ImmutableVec3 pos : targetPath)
						if (pos != null)
							drawPath(pos);
				}
				catch (final Exception e)
				{
					// it seems sometime there is unknown interruption on these codes.
				}

			if (attackTimer.hasTimePassed(pathEspTime.get()))
			{
				targetPaths.clear();
				currentPath.clear();
			}
		}
	}

	private boolean canBlock()
	{
		return mc.getThePlayer() != null && mc.getThePlayer().getHeldItem() != null && classProvider.isItemSword(mc.getThePlayer().getHeldItem().getItem());
	}

	private List<ImmutableVec3> computePath(ImmutableVec3 from, final ImmutableVec3 to)
	{
		if (!canPassThrow(new WBlockPos(from.getX(), from.getY(), from.getZ())))
			from = from.addVector(0, 1, 0);

		final PathFinder pathfinder = new PathFinder(from, to);
		pathfinder.compute();

		int i = 0;
		ImmutableVec3 lastPath = null;
		ImmutableVec3 lastEndPath = null;
		final List<ImmutableVec3> path = new ArrayList<>();
		final List<ImmutableVec3> pathFinderPath = pathfinder.getPath();
		for (final ImmutableVec3 currentPathFinderPath : pathFinderPath)
		{
			if (i == 0 || i == pathFinderPath.size() - 1) // If the current path node is start or end node
			{
				if (lastPath != null)
					path.add(lastPath.addVector(0.5, 0, 0.5));
				path.add(currentPathFinderPath.addVector(0.5, 0, 0.5));
				lastEndPath = currentPathFinderPath;
			}
			else
			{
				boolean canContinueSearching = true;
				final float maxDashDistance = maxDashDistanceValue.get();
				if (currentPathFinderPath.squareDistanceTo(lastEndPath) > maxDashDistance * maxDashDistance)
					canContinueSearching = false;
				else
				{
					final double minX = Math.min(lastEndPath.getX(), currentPathFinderPath.getX());
					final double minY = Math.min(lastEndPath.getY(), currentPathFinderPath.getY());
					final double minZ = Math.min(lastEndPath.getZ(), currentPathFinderPath.getZ());
					final double maxX = Math.max(lastEndPath.getX(), currentPathFinderPath.getX());
					final double maxY = Math.max(lastEndPath.getY(), currentPathFinderPath.getY());
					final double maxZ = Math.max(lastEndPath.getZ(), currentPathFinderPath.getZ());
					cordsLoop:
					for (int x = (int) minX; x <= maxX; x++)
						for (int y = (int) minY; y <= maxY; y++)
							for (int z = (int) minZ; z <= maxZ; z++)
								if (!PathFinder.checkPositionValidity(x, y, z, false))
								{
									canContinueSearching = false;
									break cordsLoop;
								}
				}
				if (!canContinueSearching)
				{
					path.add(lastPath.addVector(0.5, 0, 0.5));
					lastEndPath = lastPath;
				}
			}
			lastPath = currentPathFinderPath;
			i++;
		}
		return path;
	}

	private List<IEntityLivingBase> getTargets()
	{
		return mc.getTheWorld().getLoadedEntityList().stream().filter(classProvider::isEntityLivingBase).map(IEntity::asEntityLivingBase).filter(entity -> PlayerExtensionKt.getDistanceToEntityBox(mc.getThePlayer(), entity) <= rangeValue.get() && EntityUtils.isEnemy(entity, false) && entity.getHurtTime() <= hurtTimeValue.get()).sorted((o1, o2) -> (int) (o1.getDistanceToEntity(mc.getThePlayer()) * 1000 - o2.getDistanceToEntity(mc.getThePlayer()) * 1000)).collect(Collectors.toList());
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

	public final boolean isTarget(final IEntityLivingBase entity)
	{
		return !currentTargets.isEmpty() && IntStream.range(0, currentTargets.size() > maxTargetsValue.get() ? maxTargetsValue.get() : currentTargets.size()).anyMatch(i -> currentTargets.get(i).isEntityEqual(entity));
	}

	@Override
	public final String getTag()
	{
		return String.valueOf(maxDashDistanceValue.get());
	}
}
