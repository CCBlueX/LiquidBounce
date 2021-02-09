/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "Sprint", description = "Automatically sprints all the time.", category = ModuleCategory.MOVEMENT)
class Sprint : Module()
{
	/**
	 * Options
	 */
	val allDirectionsValue = BoolValue("AllDirections", true)

	private val blindnessValue = BoolValue("Blindness", true)
	val foodValue = BoolValue("Food", true)
	val checkServerSide = BoolValue("CheckServerSide", false)
	val checkServerSideGround = BoolValue("CheckServerSideOnlyGround", false)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		val blindCheck = blindnessValue.get() && thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS))
		val foodCheck = foodValue.get() && !(thePlayer.foodStats.foodLevel > 6.0f || thePlayer.capabilities.allowFlying)
		val serversideCheck = checkServerSide.get() && (thePlayer.onGround || !checkServerSideGround.get()) && !allDirectionsValue.get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(
			Rotation(
				thePlayer.rotationYaw, thePlayer.rotationPitch
			)
		) > 30

		if (!isMoving(thePlayer) || thePlayer.sneaking || blindCheck || foodCheck || serversideCheck)
		{
			thePlayer.sprinting = false
			return
		}

		if (allDirectionsValue.get() || thePlayer.movementInput.moveForward >= 0.8f) thePlayer.sprinting = true
	}

	override val tag: String?
		get() = if (allDirectionsValue.get()) "AllDirection" else null
}
