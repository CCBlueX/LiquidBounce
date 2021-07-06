/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "Rotations", description = "Allows you to see server-sided head and body rotations.", category = ModuleCategory.RENDER)
class Rotations : Module()
{
	val bodyValue = BoolValue("Body", true)
	val interpolateRotationsValue = BoolValue("Interpolate", true)
	private val onlyWhileRotatingValue = BoolValue("OnlyWhileRotating", true)

	/**
	 * Rotations - Head only, Yaw
	 */
	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (bodyValue.get() || !isRotating(thePlayer)) return

		// Head Rotations
		thePlayer.rotationYawHead = if (interpolateRotationsValue.get()) interpolateRotation(RotationUtils.lastServerRotation.yaw, RotationUtils.serverRotation.yaw, event.partialTicks) else RotationUtils.serverRotation.yaw
	}

	private fun getState(module: Class<*>) = LiquidBounce.moduleManager[module].state

	fun isRotating(thePlayer: IEntityLivingBase): Boolean
	{
		if (!onlyWhileRotatingValue.get()) return true

		val moduleManager = LiquidBounce.moduleManager

		val bowAimbot = moduleManager[BowAimbot::class.java] as BowAimbot
		val fucker = moduleManager[Fucker::class.java] as Fucker
		val civBreak = moduleManager[CivBreak::class.java] as CivBreak
		val nuker = moduleManager[Nuker::class.java] as Nuker
		val chestAura = moduleManager[ChestAura::class.java] as ChestAura
		val fly = moduleManager[Fly::class.java] as Fly

		val scaffoldState = getState(Scaffold::class.java)
		val towerState = getState(Tower::class.java)
		val killauraState = getState(KillAura::class.java)
		val derpState = getState(Derp::class.java)
		val bowAimbotState = bowAimbot.state && bowAimbot.hasTarget(thePlayer)
		val fuckerState = fucker.state && fucker.currentPos != null
		val civBreakState = civBreak.state && civBreak.blockPos != null
		val nukerState = nuker.state && nuker.currentBlock != null
		val chestAuraState = chestAura.state && chestAura.currentBlock != null
		val flyState = fly.state && fly.modeValue.get().equals("FreeHypixel", ignoreCase = true) && !fly.freeHypixelTimer.hasTimePassed(10)

		return scaffoldState || towerState || killauraState || derpState || bowAimbotState || fuckerState || civBreakState || nukerState || chestAuraState || flyState
	}

	companion object
	{
		fun interpolateRotation(prev: Float, current: Float, partialTicks: Float): Float
		{
			var delta = current - prev

			while (delta < -180.0f) delta += 360.0f
			while (delta >= 180.0f) delta -= 360.0f

			return prev + delta * partialTicks
		}
	}

	override val tag: String
		get() = if (bodyValue.get()) "Body and Head" else "Head"
}
