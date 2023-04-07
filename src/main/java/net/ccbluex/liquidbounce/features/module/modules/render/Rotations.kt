/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer

@ModuleInfo(name = "Rotations", description = "Allows you to see server-sided head and body rotations.", category = ModuleCategory.RENDER)
object Rotations : Module() {

    private val bodyValue = BoolValue("Body", true)

    private var playerYaw: Float? = null

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        mc.thePlayer?.rotationYawHead = serverRotation.yaw
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.thePlayer

        if (!bodyValue.get() || !shouldRotate() || thePlayer == null)
            return

        val packet = event.packet

        if (packet is C03PacketPlayer && packet.rotating) {
            playerYaw = packet.yaw
            mc.thePlayer.renderYawOffset = packet.yaw
            mc.thePlayer.rotationYawHead = packet.yaw
        } else {
            if (playerYaw != null)
                thePlayer.renderYawOffset = playerYaw!!

            thePlayer.rotationYawHead = thePlayer.renderYawOffset
        }
    }

    private fun shouldRotate() =
        Scaffold.state || Tower.state || (KillAura.state && KillAura.target != null) || Derp.state || BowAimbot.state
                || Fucker.state || CivBreak.state || Nuker.state || ChestAura.state
}
