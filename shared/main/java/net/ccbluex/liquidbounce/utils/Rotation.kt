/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import kotlin.math.sqrt

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) : MinecraftInstance()
{

	/**
	 * Set rotations to [player]
	 */
	fun applyRotationToPlayer(player: IEntityPlayer)
	{
		if (yaw.isNaN() || pitch.isNaN()) return

		fixedSensitivity(mc.gameSettings.mouseSensitivity)

		player.prevRotationYaw = player.rotationYaw
		player.prevRotationPitch = player.rotationPitch

		player.rotationYaw = yaw
		player.rotationPitch = pitch
	}

	/**
	 * Patch gcd exploit in aim
	 *
	 * @see net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender
	 */
	fun fixedSensitivity(sensitivity: Float)
	{
		val sensitivityModifier = sensitivity * 0.6F + 0.2F
		val gcd = sensitivityModifier * sensitivityModifier * sensitivityModifier * 1.2F // * 8.0F

		// get previous rotation
		val rotation = RotationUtils.serverRotation

		// fix yaw
		var deltaYaw = yaw - rotation.yaw
		deltaYaw -= deltaYaw % gcd
		yaw = rotation.yaw + deltaYaw

		// fix pitch
		var deltaPitch = pitch - rotation.pitch
		deltaPitch -= deltaPitch % gcd
		pitch = rotation.pitch + deltaPitch
	}

	/**
	 * Apply strafe to player
	 *
	 * @author bestnub
	 */
	fun applyStrafeToPlayer(event: StrafeEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val facing = ((WMathHelper.wrapAngleTo180_float(thePlayer.rotationYaw - yaw - 23.5f - 135) + 180) / 45).toInt()

		val yaw = yaw

		val strafe = event.strafe
		val forward = event.forward
		val friction = event.friction

		var calcForward = 0f
		var calcStrafe = 0f

		when (facing)
		{
			0 ->
			{
				calcForward = forward
				calcStrafe = strafe
			}

			1 ->
			{
				calcForward += forward
				calcStrafe -= forward
				calcForward += strafe
				calcStrafe += strafe
			}

			2 ->
			{
				calcForward = strafe
				calcStrafe = -forward
			}

			3 ->
			{
				calcForward -= forward
				calcStrafe -= forward
				calcForward += strafe
				calcStrafe -= strafe
			}

			4 ->
			{
				calcForward = -forward
				calcStrafe = -strafe
			}

			5 ->
			{
				calcForward -= forward
				calcStrafe += forward
				calcForward -= strafe
				calcStrafe -= strafe
			}

			6 ->
			{
				calcForward = -strafe
				calcStrafe = forward
			}

			7 ->
			{
				calcForward += forward
				calcStrafe += forward
				calcForward -= strafe
				calcStrafe += strafe
			}
		}

		if (calcForward > 1f || calcForward < 0.9f && calcForward > 0.3f || calcForward < -1f || calcForward > -0.9f && calcForward < -0.3f) calcForward *= 0.5f

		if (calcStrafe > 1f || calcStrafe < 0.9f && calcStrafe > 0.3f || calcStrafe < -1f || calcStrafe > -0.9f && calcStrafe < -0.3f) calcStrafe *= 0.5f

		var speed = calcStrafe * calcStrafe + calcForward * calcForward

		if (speed >= 1.0E-4f)
		{
			speed = sqrt(speed)

			if (speed < 1.0f) speed = 1.0f

			speed = friction / speed
			calcStrafe *= speed
			calcForward *= speed

			val yawRadians = WMathHelper.toRadians(yaw)
			val yawSin = WMathHelper.sin(yawRadians)
			val yawCos = WMathHelper.cos(yawRadians)

			thePlayer.motionX += calcStrafe * yawCos - calcForward * yawSin
			thePlayer.motionZ += calcForward * yawCos + calcStrafe * yawSin
		}
	}
}

/**
 * Rotation with vector
 */
data class VecRotation(val vec: WVec3, val rotation: Rotation, val face: IEnumFacing? = null)

/**
 * Rotation with place info
 */
data class PlaceRotation(val placeInfo: PlaceInfo, val rotation: Rotation)
