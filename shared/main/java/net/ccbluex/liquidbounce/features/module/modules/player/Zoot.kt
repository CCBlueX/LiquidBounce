/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.value.BoolValue

// TODO: Max packets per tick limit
@ModuleInfo(name = "Zoot", description = "Removes all bad potion effects/fire.", category = ModuleCategory.PLAYER)
class Zoot : Module()
{
	private val badEffectsValue = BoolValue("BadEffects", true)
	private val fireValue = BoolValue("Fire", true)
	private val noAirValue = BoolValue("NoAir", false)
	private val noMoveValue = BoolValue("NoMove", false)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (noMoveValue.get() && MovementUtils.isMoving(thePlayer)) return

		val onGround = thePlayer.onGround

		if (noAirValue.get() && !onGround) return

		val netHandler = mc.netHandler

		val provider = classProvider

		if (badEffectsValue.get()) thePlayer.activePotionEffects.filter { isBadEffect(it.potionID) }.maxBy(IPotionEffect::duration)?.let { effect ->
			WorkerUtils.workers.execute {
				repeat(effect.duration / 20) {
					netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
				}
			}
		}


		if (fireValue.get() && !thePlayer.capabilities.isCreativeMode && thePlayer.burning)
		{
			WorkerUtils.workers.execute {
				repeat(9) {
					netHandler.addToSendQueue(provider.createCPacketPlayer(onGround))
				}
			}
		}
	}

	private fun isBadEffect(potionID: Int): Boolean = badEffectsArray.any { potionID == it }

	companion object
	{
		val badEffectsArray = arrayListOf<Int>()

		init
		{
			val provider = classProvider

			badEffectsArray.add(provider.getPotionEnum(PotionType.HUNGER).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.MOVE_SLOWDOWN).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.DIG_SLOWDOWN).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.HARM).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.CONFUSION).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.BLINDNESS).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.WEAKNESS).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.WITHER).id)
			badEffectsArray.add(provider.getPotionEnum(PotionType.POISON).id)
		}
	}
}
