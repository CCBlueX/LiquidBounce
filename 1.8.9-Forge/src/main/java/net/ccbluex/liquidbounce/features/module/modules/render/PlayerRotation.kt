/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook


@ModuleInfo(name = "PlayerRotations", description = "Allows you to see server-sided head and body rotations.", category = ModuleCategory.RENDER)
class PlayerRotation : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Head", "Body"), "Head")

    private var playerYaw: Float? = null

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (RotationUtils.serverRotation != null && modeValue.get().equals("Head"))
            mc.thePlayer.rotationYawHead = RotationUtils.serverRotation.yaw
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (!modeValue.get().equals("Body") || !shouldRotate()) return
        val packet = event.packet
        if (packet is C03PacketPlayer.C06PacketPlayerPosLook || packet is C03PacketPlayer.C05PacketPlayerLook || packet is S08PacketPlayerPosLook) {
            playerYaw = (packet as C03PacketPlayer).yaw
            mc.thePlayer.renderYawOffset = packet.getYaw();
            mc.thePlayer.rotationYawHead = packet.getYaw();
        } else {
            mc.thePlayer.renderYawOffset = this.playerYaw!!;
            mc.thePlayer.rotationYawHead = mc.thePlayer.renderYawOffset;
        }
    }

    private fun getModuleState(module: Class<*>): Boolean {
        return LiquidBounce.moduleManager[module]!!.state
    }

    private fun shouldRotate(): Boolean {
        val killAura = LiquidBounce.moduleManager.getModule(KillAura::class.java) as KillAura
        return getModuleState(Scaffold::class.java) || getModuleState(Speed::class.java) || getModuleState(OldScaffold::class.java) || getModuleState(Tower::class.java) || (getModuleState(KillAura::class.java) && killAura.target != null) || getModuleState(Derp::class.java) || getModuleState(BowAimbot::class.java) || getModuleState(Fucker::class.java) || getModuleState(CivBreak::class.java) || getModuleState(Nuker::class.java) || getModuleState(ChestAura::class.java)
    }
}