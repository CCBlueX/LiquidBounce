/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object AutoLeave : Module("AutoLeave", Category.COMBAT, subjective = true, hideModule = false) {
    private val health by FloatValue("Health", 8f, 0f..20f)
    private val mode by ListValue("Mode", arrayOf("Quit", "InvalidPacket", "SelfHurt", "IllegalChat"), "Quit")

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (thePlayer.health <= health && !theplayer.abilities.isCreativeMode && !mc.isIntegratedServerRunning) {
            when (mode.lowercase()) {
                "quit" -> mc.world.sendQuittingDisconnectingPacket()
                "invalidpacket" -> sendPacket(C04PacketPlayerPosition(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.player.onGround))
                "selfhurt" -> sendPacket(C02PacketUseEntity(mc.player, ATTACK))
                "illegalchat" -> thePlayer.sendChatMessage(nextInt().toString() + "§§§" + nextInt())
            }

            state = false
        }
    }
}