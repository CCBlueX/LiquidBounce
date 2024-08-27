/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.option.GameOptions
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SNEAKING
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SNEAKING

object Sneak : Module("Sneak", Category.MOVEMENT, hideModule = false) {

    val mode by ListValue("Mode", arrayOf("Legit", "Vanilla", "Switch", "MineSecure"), "MineSecure")
    val stopMove by BoolValue("StopMove", false)

    private var sneaking = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (stopMove && isMoving) {
            if (sneaking)
                onDisable()
            return
        }

        when (mode.lowercase()) {
            "legit" -> mc.gameSettings.keyBindSneak.pressed = true
            "vanilla" -> {
                if (sneaking)
                    return

                sendPacket(ClientCommandC2SPacket(mc.player, START_SNEAKING))
            }

            "switch" -> {
                when (event.eventState) {
                    EventState.PRE -> {
                        sendPackets(
                            ClientCommandC2SPacket(mc.player, START_SNEAKING),
                            ClientCommandC2SPacket(mc.player, STOP_SNEAKING)
                        )
                    }
                    EventState.POST -> {
                        sendPackets(
                            ClientCommandC2SPacket(mc.player, STOP_SNEAKING),
                            ClientCommandC2SPacket(mc.player, START_SNEAKING)
                        )
                    }
                }
            }

            "minesecure" -> {
                if (event.eventState == EventState.PRE)
                    return

                sendPacket(ClientCommandC2SPacket(mc.player, START_SNEAKING))
            }
        }
    }

    @EventTarget
    fun onWorld(worldEvent: WorldEvent) {
        sneaking = false
    }

    override fun onDisable() {
        val player = mc.player ?: return

        when (mode.lowercase()) {
            "legit" -> {
                if (!GameOptions.isKeyDown(mc.gameSettings.keyBindSneak)) {
                    mc.gameSettings.keyBindSneak.pressed = false
                }
            }
            "vanilla", "switch", "minesecure" -> sendPacket(ClientCommandC2SPacket(player, STOP_SNEAKING))
        }
        sneaking = false
    }
}