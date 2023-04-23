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
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

object Zoot : Module("Zoot", ModuleCategory.PLAYER) {

    private val badEffects by BoolValue("BadEffects", true)
    private val fire by BoolValue("Fire", true)
    private val noAir by BoolValue("NoAir", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (noAir && !thePlayer.onGround)
            return

        if (badEffects) {
            val effect = thePlayer.activePotionEffects.maxByOrNull { it.duration }

            if (effect != null) {
                repeat(effect.duration / 20) {
                    sendPacket(C03PacketPlayer(thePlayer.onGround))
                }
            }
        }


        if (fire && !thePlayer.capabilities.isCreativeMode && thePlayer.isBurning) {
            repeat(9) {
                sendPacket(C03PacketPlayer(thePlayer.onGround))
            }
        }
    }

    // TODO: Check current potion
    private fun hasBadEffect() = mc.thePlayer.isPotionActive(Potion.hunger) || mc.thePlayer.isPotionActive(Potion.moveSlowdown) ||
            mc.thePlayer.isPotionActive(Potion.digSlowdown) || mc.thePlayer.isPotionActive(Potion.harm) ||
            mc.thePlayer.isPotionActive(Potion.confusion) || mc.thePlayer.isPotionActive(Potion.blindness) ||
            mc.thePlayer.isPotionActive(Potion.weakness) || mc.thePlayer.isPotionActive(Potion.wither) || mc.thePlayer.isPotionActive(Potion.poison)

}