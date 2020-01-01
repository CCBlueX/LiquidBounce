package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "Zoot", description = "Removes all bad potion effects/fire.", category = ModuleCategory.PLAYER)
class Zoot : Module() {

    private val badEffectsValue = BoolValue("BadEffects", true)
    private val fireValue = BoolValue("Fire", true)
    private val noAirValue = BoolValue("NoAir", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (noAirValue.get() && !mc.thePlayer.onGround)
            return

        if (badEffectsValue.get())
            for (potion in mc.thePlayer.activePotionEffects)
                if (potion != null && hasBadEffect()) // TODO: Check current potion
                    for (i in 0 until potion.duration / 20)
                        mc.netHandler.addToSendQueue(C03PacketPlayer())

        if (fireValue.get())
            if (!mc.thePlayer.capabilities.isCreativeMode && mc.thePlayer.isBurning)
                for (i in 0..9)
                    mc.netHandler.addToSendQueue(C03PacketPlayer())
    }

    // TODO: Check current potion
    private fun hasBadEffect() = mc.thePlayer.isPotionActive(Potion.hunger) || mc.thePlayer.isPotionActive(Potion.moveSlowdown) ||
            mc.thePlayer.isPotionActive(Potion.digSlowdown) || mc.thePlayer.isPotionActive(Potion.harm) ||
            mc.thePlayer.isPotionActive(Potion.confusion) || mc.thePlayer.isPotionActive(Potion.blindness) ||
            mc.thePlayer.isPotionActive(Potion.weakness) || mc.thePlayer.isPotionActive(Potion.wither) || mc.thePlayer.isPotionActive(Potion.poison)

}