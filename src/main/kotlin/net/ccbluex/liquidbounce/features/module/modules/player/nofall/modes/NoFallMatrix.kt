package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import kotlin.math.abs

/**
 * Bypassing Matrix AntiCheat(7/14/2025)
 * Testing on Loyisa
 * @from https://github.com/UnlegitMinecraft/FDPClientChina/blob/main/src/main/java/net/ccbluex/liquidbounce/features/module/modules/player/nofalls/matrix/Matrix663Nofall.kt
 */

object NoFallMatrix : Choice("Matrix") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private val matrixSafe by boolean("SafeNoFall", true)

    private var matrixSend = false
    private var firstNoFall = true
    private var nearGround = false

    override fun enable() {
        matrixSend = false
        firstNoFall = true
        nearGround = false
    }

    val repeatable = tickHandler {
        val fallingPlayer = FallingPlayer.fromPlayer(player)
        val collLoc = fallingPlayer.findCollision(60)

        if (player.fallDistance - player.velocity.y > 3 || (abs((collLoc?.pos?.y ?: 0) - player.y) < 3 && player.fallDistance - player.velocity.y > 2)) {
            player.fallDistance = 0.0f
            matrixSend = true

            if (matrixSafe) {
                Timer.requestTimerSpeed(0.3f, Priority.NOT_IMPORTANT, ModuleNoFall)
                player.velocity.x *= 0.5
                player.velocity.z *= 0.5
            } else {
                Timer.requestTimerSpeed(0.5f, Priority.NOT_IMPORTANT, ModuleNoFall)
            }
        } else {
            Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, ModuleNoFall)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && matrixSend) {
            matrixSend = false
            val fallingPlayer = FallingPlayer.fromPlayer(player)
            val collLoc = fallingPlayer.findCollision(60) // null -> too far to calc or fall pos in void
            if (abs((collLoc?.pos?.y ?: 0) - player.y) > 2) {
                event.cancelEvent()
                network.run {
                    sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            packet.x,
                            packet.y,
                            packet.z,
                            true,
                            player.horizontalCollision
                        )
                    )
                    sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            packet.x,
                            packet.y,
                            packet.z,
                            false,
                            player.horizontalCollision
                        )
                    )
                }
            }
        }
    }

    override fun disable() {
        Timer.requestTimerSpeed(1f, Priority.NOT_IMPORTANT, ModuleNoFall)
    }
}
