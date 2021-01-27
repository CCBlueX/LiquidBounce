/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc;

import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition;
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition.WMovingObjectType;
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos;
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

import org.jetbrains.annotations.Nullable;

public class FallingPlayer extends MinecraftInstance
{

	private double x;
	private double y;
	private double z;

	private double motionX;
	private double motionY;
	private double motionZ;

	private final float yaw;

	private float strafe;
	private float forward;

	public FallingPlayer(final double x, final double y, final double z, final double motionX, final double motionY, final double motionZ, final float yaw, final float strafe, final float forward)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
		this.yaw = yaw;
		this.strafe = strafe;
		this.forward = forward;
	}

	private void calculateForTick()
	{
		strafe *= 0.98F;
		forward *= 0.98F;

		float v = strafe * strafe + forward * forward;

		if (v >= 0.0001f)
		{
			v = (float) Math.sqrt(v);

			if (v < 1.0F)
				v = 1.0F;

			v = mc.getThePlayer().getJumpMovementFactor() / v;
			strafe *= v;
			forward *= v;
			final float f1 = functions.sin(yaw * (float) Math.PI / 180.0F);
			final float f2 = functions.cos(yaw * (float) Math.PI / 180.0F);
			motionX += strafe * f2 - forward * f1;
			motionZ += forward * f2 + strafe * f1;
		}

		motionY -= 0.08;

		motionX *= 0.91;
		motionY *= 0.9800000190734863D;
		motionY *= 0.91;
		motionZ *= 0.91;

		x += motionX;
		y += motionY;
		z += motionZ;
	}

	public CollisionResult findCollision(final int ticks)
	{
		final WVec3 start = new WVec3(x, y, z);
		final WVec3 end = new WVec3(x, y, z);

		for (int i = 0; i < ticks; i++)
		{
			calculateForTick();

			WBlockPos raytracedBlock;
			final float w = mc.getThePlayer().getWidth() / 2.0F;

			if ((raytracedBlock = rayTrace(start, end)) != null)
				return new CollisionResult(raytracedBlock, i);

			if ((raytracedBlock = rayTrace(start.addVector(w, 0, w), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(-w, 0, w), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(w, 0, -w), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(-w, 0, -w), end)) != null)
				return new CollisionResult(raytracedBlock, i);

			if ((raytracedBlock = rayTrace(start.addVector(w, 0, w / 2.0f), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(-w, 0, w / 2.0f), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(w / 2.0f, 0, w), end)) != null)
				return new CollisionResult(raytracedBlock, i);
			if ((raytracedBlock = rayTrace(start.addVector(w / 2.0f, 0, -w), end)) != null)
				return new CollisionResult(raytracedBlock, i);
		}

		return null;
	}

	@Nullable
	private static WBlockPos rayTrace(final WVec3 start, final WVec3 end)
	{
		final IMovingObjectPosition result = mc.getTheWorld().rayTraceBlocks(start, end, true);

		return result != null && result.getTypeOfHit() == WMovingObjectType.BLOCK && result.getSideHit().isUp() ? result.getBlockPos() : null;
	}

	public static class CollisionResult
	{
		private final WBlockPos pos;
		private final int tick;

		public CollisionResult(final WBlockPos pos, final int tick)
		{
			this.pos = pos;
			this.tick = tick;
		}

		public WBlockPos getPos()
		{
			return pos;
		}

		public int getTick()
		{
			return tick;
		}
	}
}
