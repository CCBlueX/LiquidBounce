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
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Action.ATTACK
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object AutoLeave : Module("AutoLeave", Category.COMBAT, subjective = true, hideModule = false) {
    private val health by FloatValue("Health", 8f, 0f..20f)
    private val mode by ListValue("Mode", arrayOf("Quit", "InvalidPacket", "SelfHurt", "IllegalChat"), "Quit")

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (thePlayer.health <= health && !theplayer.abilities.isCreativeMode && !mc.isIntegratedServerRunning) {
            when (mode.lowercase()) {
                "quit" -> mc.world.sendQuittingDisconnectingPacket()
                "invalidpacket" -> sendPacket(PositionOnly(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.player.onGround))
                "selfhurt" -> sendPacket(PlayerInteractEntityC2SPacket(mc.player, ATTACK))
                "illegalchat" -> thePlayer.sendChatMessage(nextInt().toString() + "§§§" + nextInt())
            }

            state = false
        }
    }
}