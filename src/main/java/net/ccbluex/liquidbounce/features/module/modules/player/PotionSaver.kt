/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.*

@ModuleInfo(name = "PotionSaver", description = "Freezes all potion effects while you are standing still.", category = ModuleCategory.PLAYER)
class PotionSaver : Module() {

    @EventTarget
    fun onPacket(e: PacketEvent) {
        val packet = e.packet

        if (packet is C03PacketPlayer && packet !is C04PacketPlayerPosition && packet !is C06PacketPlayerPosLook &&
            packet !is C05PacketPlayerLook && mc.thePlayer != null && !mc.thePlayer.isUsingItem)
            e.cancelEvent()
    }

}