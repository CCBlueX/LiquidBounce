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
import java.util.stream.Stream

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

		if (noMoveValue.get() && MovementUtils.isMoving) return
		if (noAirValue.get() && !thePlayer.onGround) return

		if (badEffectsValue.get())
		{
			val effect = thePlayer.activePotionEffects.asSequence().filter(::isBadEffect).maxBy(IPotionEffect::duration)

			if (effect != null)
			{
				WorkerUtils.workers.submit {
					repeat(effect.duration / 20) {
						mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(thePlayer.onGround))
					}
				}
			}
		}


		if (fireValue.get() && !thePlayer.capabilities.isCreativeMode && thePlayer.burning)
		{
			WorkerUtils.workers.submit {
				repeat(9) {
					mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(thePlayer.onGround))
				}
			}
		}
	}

	// TODO: Check current potion
	private fun isBadEffect(effect: IPotionEffect): Boolean
	{
		return Stream.of(

			classProvider.getPotionEnum(PotionType.HUNGER),
			classProvider.getPotionEnum(PotionType.MOVE_SLOWDOWN),
			classProvider.getPotionEnum(PotionType.DIG_SLOWDOWN),
			classProvider.getPotionEnum(PotionType.HARM),
			classProvider.getPotionEnum(PotionType.CONFUSION),
			classProvider.getPotionEnum(PotionType.BLINDNESS),
			classProvider.getPotionEnum(PotionType.WEAKNESS),
			classProvider.getPotionEnum(PotionType.WITHER),
			classProvider.getPotionEnum(PotionType.POISON)

		).anyMatch { effect.potionID == it.id }
	}
}
