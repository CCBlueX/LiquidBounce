/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import static java.lang.Math.*;

import java.util.Optional;
import java.util.Random;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayer;
import net.ccbluex.liquidbounce.api.minecraft.util.*;
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition.WMovingObjectType;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;

public final class RotationUtils extends MinecraftInstance implements Listenable
{

	private static final Random random = new Random();

	private static int keepLength;
	private static float minResetTurnSpeed = 180.0f;
	private static float maxResetTurnSpeed = 180.0f;

	public static Rotation targetRotation;
	public static Rotation serverRotation = new Rotation(0.0F, 0.0F);
	public static Rotation lastServerRotation = new Rotation(0.0F, 0.0F);

	public static boolean keepCurrentRotation;

	private static double x = random.nextDouble();
	private static double y = random.nextDouble();
	private static double z = random.nextDouble();

	/**
	 * Face block
	 *
	 * @param blockPos
	 *                 target block
	 */
	public static VecRotation faceBlock(final WBlockPos blockPos)
	{
		if (blockPos == null)
			return null;

		VecRotation vecRotation = null;
		final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

		for (double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D)
			for (double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D)
				for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D)
				{
					final WVec3 posVec = new WVec3(blockPos).addVector(xSearch, ySearch, zSearch);
					final double dist = eyesPos.distanceTo(posVec);

					final double diffX = posVec.getXCoord() - eyesPos.getXCoord();
					final double diffY = posVec.getYCoord() - eyesPos.getYCoord();
					final double diffZ = posVec.getZCoord() - eyesPos.getZCoord();

					final double diffXZ = StrictMath.hypot(diffX, diffZ);

					final Rotation rotation = new Rotation(WMathHelper.wrapAngleTo180_float(WMathHelper.toDegrees((float) StrictMath.atan2(diffZ, diffX)) - 90.0F), WMathHelper.wrapAngleTo180_float(-WMathHelper.toDegrees((float) StrictMath.atan2(diffY, diffXZ))));

					final WVec3 rotationVector = getVectorForRotation(rotation);
					final WVec3 vector = eyesPos.addVector(rotationVector.getXCoord() * dist, rotationVector.getYCoord() * dist, rotationVector.getZCoord() * dist);
					final IMovingObjectPosition obj = mc.getTheWorld().rayTraceBlocks(eyesPos, vector, false, false, true);

					if (obj != null && obj.getTypeOfHit() == WMovingObjectType.BLOCK)
					{
						final VecRotation currentVec = new VecRotation(posVec, rotation);

						if (vecRotation == null || getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))
							vecRotation = currentVec;
					}
				}

		return vecRotation;
	}

	/**
	 * Face target with bow
	 *
	 * @param target
	 *                         your enemy
	 * @param silent
	 *                         client side rotations
	 * @param enemyPrediction
	 *                         enemyPrediction new enemy position
	 * @param playerPrediction
	 *                         enemyPrediction new player position
	 */
	public static void faceBow(final IEntity target, final boolean silent, final boolean enemyPrediction, final boolean playerPrediction, final float minTurnSpeed, final float maxTurnSpeed, final float minSmoothingRatio, final float maxSmoothingRatio)
	{
		final IEntityPlayerSP player = mc.getThePlayer();

		// Prediction
		final double xDelta = (target.getPosX() - target.getLastTickPosX()) * 0.4;
		final double yDelta = (target.getPosY() - target.getLastTickPosY()) * 0.4;
		final double zDelta = (target.getPosZ() - target.getLastTickPosZ()) * 0.4;
		double distance = player.getDistanceToEntity(target);
		distance -= distance % 0.8;

		final boolean sprinting = target.isSprinting();

		final double xPrediction = distance / 0.8 * xDelta * (sprinting ? 1.25 : 1);
		final double yPrediction = distance / 0.8 * yDelta;
		final double zPrediction = distance / 0.8 * zDelta * (sprinting ? 1.25 : 1);

		// Calculate the (predicted) target position
		final double posX = target.getPosX() + (enemyPrediction ? xPrediction : 0) - (player.getPosX() + (playerPrediction ? player.getPosX() - player.getPrevPosX() : 0));
		final double posY = target.getEntityBoundingBox().getMinY() + (enemyPrediction ? yPrediction : 0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().getMinY() + (enemyPrediction ? player.getPosY() - player.getPrevPosY() : 0)) - player.getEyeHeight();
		final double posZ = target.getPosZ() + (enemyPrediction ? zPrediction : 0) - (player.getPosZ() + (playerPrediction ? player.getPosZ() - player.getPrevPosZ() : 0));

		// Bow Power Calculation
		final FastBow fastBow = (FastBow) LiquidBounce.moduleManager.get(FastBow.class);
		float velocity = (fastBow.getState() ? fastBow.getPacketsValue().get() : player.getItemInUseDuration()) / 20.0f;
		velocity = (velocity * velocity + velocity * 2) / 3;

		if (velocity > 1)
			velocity = 1;

		// Calculate Rotation
		final double posSqrt = StrictMath.hypot(posX, posZ);
		final Rotation rotation = new Rotation(WMathHelper.toDegrees((float) StrictMath.atan2(posZ, posX)) - 90, -WMathHelper.toDegrees((float) StrictMath.atan((velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006F * (0.006F * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006F * posSqrt))));
		final Rotation limitedRotation = limitAngleChange(new Rotation(player.getRotationYaw(), player.getRotationPitch()), rotation, RandomUtils.nextFloat(min(minTurnSpeed, maxTurnSpeed), max(minTurnSpeed, maxTurnSpeed)), RandomUtils.nextFloat(min(minSmoothingRatio, maxSmoothingRatio), max(minSmoothingRatio, maxSmoothingRatio)));

		// Apply Rotation
		if (silent)
		{
			setTargetRotation(limitedRotation);
			setNextResetTurnSpeed(minTurnSpeed, maxTurnSpeed);
		}
		else
			limitedRotation.applyRotationToPlayer(mc.getThePlayer());
	}

	/**
	 * Translate vec to rotation
	 *
	 * @param  vec
	 *                       target vec
	 * @param  playerPredict
	 *                       predict new location of your body
	 * @return               rotation
	 */
	public static Rotation toRotation(final WVec3 vec, final boolean playerPredict)
	{
		final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

		if (playerPredict)
			eyesPos.addVector(mc.getThePlayer().getMotionX(), mc.getThePlayer().getMotionY(), mc.getThePlayer().getMotionZ());

		final double diffX = vec.getXCoord() - eyesPos.getXCoord();
		final double diffY = vec.getYCoord() - eyesPos.getYCoord();
		final double diffZ = vec.getZCoord() - eyesPos.getZCoord();

		return new Rotation(WMathHelper.wrapAngleTo180_float(WMathHelper.toDegrees((float) StrictMath.atan2(diffZ, diffX)) - 90.0F), WMathHelper.wrapAngleTo180_float(-WMathHelper.toDegrees((float) StrictMath.atan2(diffY, StrictMath.hypot(diffX, diffZ)))));
	}

	/**
	 * Get the center of a box
	 *
	 * @param  box
	 *             your box
	 * @return     center of box
	 */
	public static WVec3 getCenter(final IAxisAlignedBB box)
	{
		return new WVec3(box.getMinX() + (box.getMaxX() - box.getMinX()) * 0.5, box.getMinY() + (box.getMaxY() - box.getMinY()) * 0.5, box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * 0.5);
	}

	public enum SearchCenterMode
	{
		SEARCH_GOOD_CENTER,
		LOCK_CENTER,
		OUT_BORDER,
		RANDOM_GOOD_CENTER
	}

	public static class JitterData
	{
		public final int yawRate;
		public final int pitchRate;
		public final float minYaw;
		public final float maxYaw;
		public final float minPitch;
		public final float maxPitch;

		public JitterData(final int yawRate, final int pitchRate, final float minYaw, final float maxYaw, final float minPitch, final float maxPitch)
		{
			this.yawRate = yawRate;
			this.pitchRate = pitchRate;
			this.minYaw = minYaw;
			this.maxYaw = maxYaw;
			this.minPitch = minPitch;
			this.maxPitch = maxPitch;
		}
	}

	/**
	 * Search good center
	 *
	 * @param  box
	 *                           enemy box
	 * @param  mode
	 *                           search center mode
	 * @param  jitter
	 *                           jitter option
	 * @param  jitterData
	 *                           jitter data option (minyawspeed, minpitchspeed etc.)
	 * @param  playerPrediction
	 *                           predict option
	 * @param  throughWalls
	 *                           throughWalls option
	 * @param  distance
	 *                           vec3 distance limit
	 * @param  hitboxDecrement
	 *                           decrement of the entity hitbox size. default is 0.2D
	 * @param  searchSensitivity
	 *                           count of step to search the good center. (*Warning If you set this value too low, it will make your minecraft SO SLOW AND SLOW.*) default is 0.2D
	 * @return                   center
	 */
	public static VecRotation searchCenter(final IAxisAlignedBB box, final SearchCenterMode mode, final boolean jitter, final JitterData jitterData, final boolean playerPrediction, final boolean throughWalls, final float distance, final double hitboxDecrement, final double searchSensitivity)
	{
		final WVec3 randomVec;
		final WVec3 eyes = mc.getThePlayer().getPositionEyes(1.0F);

		switch (mode)
		{
			case LOCK_CENTER:
			{
				randomVec = getCenter(box);
				return new VecRotation(randomVec, toRotation(randomVec, playerPrediction));
			}
			case OUT_BORDER:
			{
				randomVec = new WVec3(box.getMinX() + (box.getMaxX() - box.getMinX()) * (x * 0.3 + 1.0), box.getMinY() + (box.getMaxY() - box.getMinY()) * (y * 0.3 + 1.0), box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * (z * 0.3 + 1.0));
				return new VecRotation(randomVec, toRotation(randomVec, playerPrediction));
			}
			default:
		}

		randomVec = new WVec3(box.getMinX() + (box.getMaxX() - box.getMinX()) * x * 0.8, box.getMinY() + (box.getMaxY() - box.getMinY()) * y * 0.8, box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * z * 0.8);
		final Rotation randomRotation = toRotation(randomVec, playerPrediction);
		float yawJitterAmount = 0, pitchJitterAmount = 0;

		// Calculate jitter amount
		if (jitter)
		{
			final boolean yawJitter = jitterData.yawRate > 0 && new Random().nextInt(100) <= jitterData.yawRate;
			final boolean pitchJitter = jitterData.pitchRate > 0 && new Random().nextInt(100) <= jitterData.pitchRate;
			final boolean yawNegative = new Random().nextBoolean();
			final boolean pitchNegative = new Random().nextBoolean();

			if (yawJitter)
				yawJitterAmount = yawNegative ? -RandomUtils.nextFloat(jitterData.minYaw, jitterData.maxYaw) : RandomUtils.nextFloat(jitterData.minYaw, jitterData.maxYaw);

			if (pitchJitter)
				pitchJitterAmount = pitchNegative ? -RandomUtils.nextFloat(jitterData.minPitch, jitterData.maxPitch) : RandomUtils.nextFloat(jitterData.minPitch, jitterData.maxPitch);
		}

		// Search boundingbox center
		VecRotation vecRotation = null;

		for (double xSearch = hitboxDecrement; xSearch < 1 - hitboxDecrement; xSearch += searchSensitivity)
			for (double ySearch = hitboxDecrement; ySearch < 1 - hitboxDecrement; ySearch += searchSensitivity)
				for (double zSearch = hitboxDecrement; zSearch < 1 - hitboxDecrement; zSearch += searchSensitivity)
				{
					final WVec3 vec3 = new WVec3(box.getMinX() + (box.getMaxX() - box.getMinX()) * xSearch, box.getMinY() + (box.getMaxY() - box.getMinY()) * ySearch, box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * zSearch);

					final double vecDist = eyes.distanceTo(vec3);

					if (vecDist > distance)
						continue;

					if (throughWalls || isVisible(vec3))
					{
						final Rotation rotation = toRotation(vec3, playerPrediction);
						final VecRotation currentVec = new VecRotation(vec3, rotation);

						if (vecRotation == null || (mode == SearchCenterMode.RANDOM_GOOD_CENTER ? getRotationDifference(currentVec.getRotation(), randomRotation) < getRotationDifference(vecRotation.getRotation(), randomRotation) : getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation())))
							vecRotation = currentVec;
					}
				}

		// Jitter
		if (vecRotation != null && jitter)
		{
			vecRotation.getRotation().setYaw(vecRotation.getRotation().getYaw() + yawJitterAmount);
			float pitch = vecRotation.getRotation().getPitch() + pitchJitterAmount;
			if (pitch > 90)
				pitch = 90;
			else if (pitch < -90)
				pitch = -90;
			vecRotation.getRotation().setPitch(pitch);
		}

		return vecRotation;
	}

	/**
	 * Calculate difference between the "client-sided rotation" and your entity
	 *
	 * @param  entity
	 *                your entity
	 * @return        difference between rotation
	 */
	public static double getClientRotationDifference(final IEntity entity)
	{
		final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

		return getRotationDifference(rotation, new Rotation(mc.getThePlayer().getRotationYaw(), mc.getThePlayer().getRotationPitch()));
	}

	/**
	 * Calculate difference between the "server-sided rotation" and your entity
	 *
	 * @param  entity
	 *                your entity
	 * @return        difference between rotation
	 */
	public static double getServerRotationDifference(final IEntity entity)
	{
		final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

		return getRotationDifference(rotation, serverRotation);
	}

	/**
	 * Calculate difference between the client rotation and your entity
	 *
	 * @param  entity
	 *                your entity
	 * @return        difference between rotation
	 */
	public static double getRotationDifference(final IEntity entity)
	{
		final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

		return getRotationDifference(rotation, new Rotation(mc.getThePlayer().getRotationYaw(), mc.getThePlayer().getRotationPitch()));
	}

	/**
	 * Calculate difference between the server rotation and your rotation
	 *
	 * @param  rotation
	 *                  your rotation
	 * @return          difference between rotation
	 */
	public static double getRotationDifference(final Rotation rotation)
	{
		return Optional.ofNullable(serverRotation).map(serverRotation1 -> getRotationDifference(rotation, serverRotation1)).orElse(0.0D);
	}

	/**
	 * Calculate difference between two rotations
	 *
	 * @param  first
	 *                rotation
	 * @param  second
	 *                rotation
	 * @return        difference between rotation
	 */
	public static double getRotationDifference(final Rotation first, final Rotation second)
	{
		return StrictMath.hypot(getAngleDifference(first.getYaw(), second.getYaw()), first.getPitch() - second.getPitch());
	}

	/**
	 * Limit your rotation using a turn speed
	 *
	 * @param  currentRotation
	 *                         your current rotation
	 * @param  targetRotation
	 *                         your goal rotation
	 * @param  turnSpeed
	 *                         your turn speed
	 * @return                 limited rotation
	 */
	public static Rotation limitAngleChange(final Rotation currentRotation, final Rotation targetRotation, final float turnSpeed, final float acceleration)
	{
		float yawDelta = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
		float pitchDelta = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());

		final float accel = min(acceleration, 1.0F);

		yawDelta -= yawDelta * accel;
		pitchDelta -= pitchDelta * accel;

		return new Rotation(currentRotation.getYaw() + (yawDelta > turnSpeed ? turnSpeed : max(yawDelta, -turnSpeed)), currentRotation.getPitch() + (pitchDelta > turnSpeed ? turnSpeed : max(pitchDelta, -turnSpeed)));
	}

	private static double getAngleDifference(final Rotation rot1, final Rotation rot2)
	{
		return StrictMath.hypot(getAngleDifference(rot1.getYaw(), rot2.getYaw()), getAngleDifference(rot1.getPitch(), rot2.getPitch()));
	}

	/**
	 * Calculate difference between two angle points
	 *
	 * @param  a
	 *           angle point
	 * @param  b
	 *           angle point
	 * @return   difference between angle points
	 */
	private static float getAngleDifference(final float a, final float b)
	{
		return ((a - b) % 360.0F + 540.0F) % 360.0F - 180.0F;
	}

	/**
	 * Calculate rotation to vector
	 *
	 * @param  rotation
	 *                  your rotation
	 * @return          target vector
	 */
	public static WVec3 getVectorForRotation(final Rotation rotation)
	{
		final float yawRadians = WMathHelper.toRadians(rotation.getYaw());
		final float pitchRadians = WMathHelper.toRadians(rotation.getPitch());

		final float yawCos = WMathHelper.cos(-yawRadians - (float) PI);
		final float yawSin = WMathHelper.sin(-yawRadians - (float) PI);
		final float pitchCos = -WMathHelper.cos(-pitchRadians);
		final float pitchSin = WMathHelper.sin(-pitchRadians);

		return new WVec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
	}

	/**
	 * Allows you to check if your crosshair is over your target entity
	 *
	 * @param  targetEntity
	 *                            your target entity
	 * @param  blockReachDistance
	 *                            your reach
	 * @return                    if crosshair is over target
	 */
	public static boolean isFaced(final IEntity targetEntity, final double blockReachDistance)
	{
		return RaycastUtils.raycastEntity(blockReachDistance, entity -> targetEntity != null && targetEntity.equals(entity)) != null;
	}

	/**
	 * Allows you to check if your enemy is behind a wall
	 */
	public static boolean isVisible(final WVec3 vec3)
	{
		final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

		return mc.getTheWorld().rayTraceBlocks(eyesPos, vec3) == null;
	}

	/**
	 * Handle minecraft tick
	 *
	 * @param event
	 *              Tick event
	 */
	@EventTarget
	public void onTick(final TickEvent event)
	{
		if (targetRotation != null)
		{
			keepLength--;

			if (keepLength <= 0)
				reset();
		}

		if (random.nextGaussian() > 0.8D)
			x = random();
		if (random.nextGaussian() > 0.8D)
			y = random();
		if (random.nextGaussian() > 0.8D)
			z = random();
	}

	/**
	 * Set your target rotation
	 *
	 * @param rotation
	 *                 your target rotation
	 */
	public static void setTargetRotation(final Rotation rotation, final int keepLength)
	{
		if (Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch()) || rotation.getPitch() > 90 || rotation.getPitch() < -90)
			return;

		rotation.fixedSensitivity(mc.getGameSettings().getMouseSensitivity());
		targetRotation = rotation;
		RotationUtils.keepLength = keepLength;
	}

	public static void setNextResetTurnSpeed(final float min, final float max)
	{
		minResetTurnSpeed = min;
		maxResetTurnSpeed = max;
	}

	/**
	 * Set your target rotation
	 *
	 * @param rotation
	 *                 your target rotation
	 */
	public static void setTargetRotation(final Rotation rotation)
	{
		setTargetRotation(rotation, 0);
	}

	/**
	 * Handle packet
	 *
	 * @param event
	 *              Packet Event
	 */
	@EventTarget
	public void onPacket(final PacketEvent event)
	{
		final IPacket packet = event.getPacket();

		if (classProvider.isCPacketPlayer(packet))
		{
			final ICPacketPlayer packetPlayer = packet.asCPacketPlayer();

			if (targetRotation != null && !keepCurrentRotation && (targetRotation.getYaw() != serverRotation.getYaw() || targetRotation.getPitch() != serverRotation.getPitch()))
			{
				packetPlayer.setYaw(targetRotation.getYaw());
				packetPlayer.setPitch(targetRotation.getPitch());
				packetPlayer.setRotating(true);
			}

			if (serverRotation != null)
				lastServerRotation = new Rotation(serverRotation.getYaw(), serverRotation.getPitch());

			if (packetPlayer.isRotating())
				serverRotation = new Rotation(packetPlayer.getYaw(), packetPlayer.getPitch());
		}
	}

	/**
	 * Reset your target rotation
	 */
	public static void reset()
	{
		keepLength = 0;
		final Rotation goalRotation = new Rotation(mc.getThePlayer().getRotationYaw(), mc.getThePlayer().getRotationPitch());
		if (minResetTurnSpeed >= 180 || getAngleDifference(targetRotation, goalRotation) <= minResetTurnSpeed)
		{
			targetRotation = null;
			// Reset the resetTurnSpeed
			minResetTurnSpeed = 180.0f;
			maxResetTurnSpeed = 180.0f;
		}
		else
		{
			final Rotation limited = limitAngleChange(targetRotation, goalRotation, maxResetTurnSpeed - minResetTurnSpeed > 0 ? RandomUtils.nextFloat(minResetTurnSpeed, maxResetTurnSpeed) : maxResetTurnSpeed, 0);
			limited.fixedSensitivity(mc.getGameSettings().getMouseSensitivity());
			targetRotation = limited;
		}
	}

	/**
	 * @return YESSSS!!!
	 */
	@Override
	public boolean handleEvents()
	{
		return true;
	}
}
