package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity

// Ported from FDPClient (https://github.com/Project-EZ4H/FDPClient)
// Original code is available in https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/player/HackerDetector.kt
@ModuleInfo(name = "LightningDetector", description = "Check lightning spawn at.", category = ModuleCategory.WORLD)
class LightningDetector : Module()
{
    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S2CPacketSpawnGlobalEntity)
        {
            if (packet.func_149053_g() != 1) return
            LiquidBounce.hud.addNotification(Notification(NotificationIcon.INFORMATION, name, "X:" + packet.func_149051_d() + " Y:" + packet.func_149050_e() + " Z:" + packet.func_149049_f(), 10000))
        }
    }
}
