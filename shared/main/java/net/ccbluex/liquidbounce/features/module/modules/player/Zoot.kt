/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

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
            val effect = thePlayer.activePotionEffects.maxBy { it.duration }

            if (effect != null) {
                repeat(effect.duration / 20) {
                    mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(thePlayer.onGround))
                }
            }
        }


        if (fireValue.get() && !thePlayer.capabilities.isCreativeMode && thePlayer.burning) {
            repeat(9) {
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(thePlayer.onGround))
            }
        }
    }

    // TODO: Check current potion
    private fun hasBadEffect(): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        return thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.HUNGER)) || thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SLOWDOWN)) ||
                thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.DIG_SLOWDOWN)) || thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.HARM)) ||
                thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.CONFUSION)) || thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS)) ||
                thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.WEAKNESS)) || thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.WITHER)) || thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.POISON))
    }

}