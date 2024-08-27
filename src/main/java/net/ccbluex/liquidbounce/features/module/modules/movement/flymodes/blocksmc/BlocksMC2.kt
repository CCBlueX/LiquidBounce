/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.boostSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.debugFly
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.extraBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.stable
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.stopOnLanding
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.stopOnNoMove
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.timerSlowed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.client.entity.ClientPlayerEntity
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.Packet
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.world.World

/**
 * Implements a flying method similar to BlocksMC Fly mode, but instead of clipping,
 * it lags (blinks) the player instead.
 *
 * Note:
 * Clipping is likely patched, as players may receive false bans if phased through a block after reaching
 * certain (VL). Prolonged flight over long distances is not recommended.
 *
 * @author EclipsesDev
 */
object BlocksMC2 : FlyMode("BlocksMC2") {

    private var flying = false
    private var isNotUnder = false
    private var isBlinked = false
    private var airborneTicks = 0
    private var jumped = false

    private val packets = mutableListOf<Packet<*>>()
    private val packetsReceived = mutableListOf<Packet<*>>()

    override fun onUpdate() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        if (flying) {
            if (player.onGround && stopOnLanding) {
                if (debugFly)
                    Chat.print("Ground Detected.. Stopping Fly")
                Fly.state = false
            }

            if (!isMoving && stopOnNoMove) {
                if (debugFly)
                    Chat.print("No Movement Detected.. Stopping Fly. (Could be flagged)")
                Fly.state = false
            }
        }

        updateOffGroundTicks(player)

        if (shouldFly(player, world)) {
            if (isBlinked) {

                if (stable)
                    player.velocityY = 0.0

                handleTimerSlow(player)
                handlePlayerFlying(player)
            } else {
                if (player.onGround)
                    strafe()
            }
        } else {
            if (debugFly)
                Chat.print("Pls stand under a block")
        }
    }

    override fun onDisable() {
        isNotUnder = false
        flying = false
        jumped = false
        isBlinked = false

        if (mc.player == null)
            return

        blink()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        Fly.state = false

        // Clear packets on disconnect
        if (event.worldClient == null) {
            packets.clear()
            packetsReceived.clear()
        }
    }

    private fun updateOffGroundTicks(player: ClientPlayerEntity) {
        airborneTicks = if (player.onGround) 0 else airborneTicks++
    }

    private fun handleTimerSlow(player: ClientPlayerEntity) {
        if (!player.onGround && timerSlowed) {
            if (player.ticksAlive % 4 == 0) {
                mc.ticker.timerSpeed = 0.45f
            } else {
                mc.ticker.timerSpeed = 0.4f
            }
        } else {
            mc.ticker.timerSpeed = 1.0f
        }
    }

    private fun shouldFly(player: ClientPlayerEntity, world: World): Boolean {
        return world.doesBoxCollide(player, player.boundingBox.offset(0.0, 0.5, 0.0)).isEmpty() || flying
    }

    private fun handlePlayerFlying(player: ClientPlayerEntity) {
        when (airborneTicks) {
            0 -> {
                if (isNotUnder) {
                    strafe(boostSpeed + extraBoost)
                    player.tryJump()
                    flying = true
                    isNotUnder = false
                }
            }
            1 -> {
                if (flying) {
                    strafe(boostSpeed)
                }
            }
        }
    }

    @EventTarget
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.player == null || mc.world == null || mc.!player.isAlive)
            return

        if (event.isCancelled)
            return

        when (packet) {
            is HandshakeC2SPacket, is QueryRequestC2SPacket, is QueryPingC2SPacket, is ChatMessageS2CPacket, is DisconnectS2CPacket -> {
                return
            }
        }

        if (!isBlinked) {

            isNotUnder = true
            isBlinked = true

            if (debugFly)
                Chat.print("blinked.. fly now!")

            if (event.eventType == EventState.RECEIVE && mc.player.ticksAlive > 10) {
                event.cancelEvent()
                synchronized(packetsReceived) {
                    packetsReceived += packet
                }
            }
            if (event.eventType == EventState.SEND) {
                synchronized(packets) {
                    sendPackets(*packets.toTypedArray(), triggerEvents = false)
                }
                packets.clear()
            }
        }
    }

    @EventTarget
    override fun onMotion(event: MotionEvent) {
        val player = mc.player ?: return

        if (!player.isAlive || mc.player.ticksAlive <= 10) {
            blink()
        }

        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }
        packets.clear()
    }

    private fun blink() {
        synchronized(packetsReceived) {
            PacketUtils.queuedPackets.addAll(packetsReceived)
        }
        synchronized(packets) {
            sendPackets(*packets.toTypedArray(), triggerEvents = false)
        }

        packets.clear()
        packetsReceived.clear()
    }
}