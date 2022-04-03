/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

@ModuleInfo(name = "Zoot", description = "Removes all bad potion effects/fire.", category = ModuleCategory.PLAYER)
class Zoot : Module() {

    private val badEffectsValue = BoolValue("BadEffects", true)
    private val fireValue = BoolValue("Fire", true)
    private val noAirValue = BoolValue("NoAir", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (noAirValue.get() && !thePlayer.onGround)
            return

        if (badEffectsValue.get()) {
            val effect = thePlayer.activePotionEffects.maxByOrNull { it.duration }

            if (effect != null) {
                repeat(effect.duration / 20) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer(thePlayer.onGround))
                }
            }
        }


        if (fireValue.get() && !thePlayer.capabilities.isCreativeMode && thePlayer.isBurning) {
            repeat(9) {
                mc.netHandler.addToSendQueue(C03PacketPlayer(thePlayer.onGround))
            }
        }
    }

    // TODO: Check current potion
    private fun hasBadEffect() = mc.thePlayer.isPotionActive(Potion.hunger) || mc.thePlayer.isPotionActive(Potion.moveSlowdown) ||
            mc.thePlayer.isPotionActive(Potion.digSlowdown) || mc.thePlayer.isPotionActive(Potion.harm) ||
            mc.thePlayer.isPotionActive(Potion.confusion) || mc.thePlayer.isPotionActive(Potion.blindness) ||
            mc.thePlayer.isPotionActive(Potion.weakness) || mc.thePlayer.isPotionActive(Potion.wither) || mc.thePlayer.isPotionActive(Potion.poison)

}