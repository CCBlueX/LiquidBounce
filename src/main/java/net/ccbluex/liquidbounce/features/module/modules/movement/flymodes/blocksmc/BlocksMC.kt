/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.WorldEvent
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
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.world.World

/**
 * Fly concept originally taken from CrossSine:
 * https://github.com/shxp3/CrossSine/blob/main/src/main/java/net/ccbluex/liquidbounce/features/module/modules/movement/flights/ncp/LatestNCP.java
 *
 * Modified by @Eclipses and mixed with code from NextGen:
 * https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/movement/fly/modes/specific/FlyNcpClip.kt
 *
 * NOTE (Before using this fly mode, READ THIS):
 * Caution: Prolonged flying over long distances is not recommended.
 *
 * Additionally, ensure that you avoid flight before you got flagged or
 * (S08 Packet) teleported, as this will flag u more, or you can wait till
 * you get the Fly message Line(153). Also avoid flying too many times (At long distance).
 *
 * @author EclipsesDev
 */
object BlocksMC : FlyMode("BlocksMC") {

    private var flying = false
    private var isNotUnder = false
    private var isTeleported = false
    private var airborneTicks = 0
    private var jumped = false

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
            if (isTeleported) {

                if (stable)
                    player.velocityY = 0.0

                handleTimerSlow(player)
                handlePlayerFlying(player)
            } else {
                if (debugFly)
                    Chat.print("Waiting to be Teleported.. Please ensure you're below a block.")
            }
        } else {
            handleTeleport(player)
        }

        strafe()
    }

    override fun onDisable() {
        isNotUnder = false
        flying = false
        isTeleported = false
        jumped = false
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        Fly.state = false
    }

    private fun updateOffGroundTicks(player: EntityPlayerSP) {
        airborneTicks = if (player.onGround) 0 else airborneTicks + 1
    }

    private fun handleTimerSlow(player: EntityPlayerSP) {
        if (!player.onGround && timerSlowed) {
            if (player.ticksAlive % 7 == 0) {
                mc.ticker.timerSpeed = 0.415f
            } else {
                mc.ticker.timerSpeed = 0.35f
            }
        } else {
            mc.ticker.timerSpeed = 1.0f
        }
    }

    private fun shouldFly(player: EntityPlayerSP, world: World): Boolean {
        return world.getCollidingBoundingBoxes(player, player.boundingBox.offset(0.0, 1.0, 0.0)).isEmpty() || flying
    }

    private fun handlePlayerFlying(player: EntityPlayerSP) {
        when (airborneTicks) {
            0 -> {
                if (isNotUnder && isTeleported) {
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

    private fun handleTeleport(player: EntityPlayerSP) {
        isNotUnder = true
        if (!isTeleported) {
            sendPackets(
                PositionOnly(
                    player.x,
                    // Clipping is now patch in BlocksMC
                    player.z - 0.05,
                    player.z,
                    false
                )
            )
            sendPackets(
                PositionOnly(
                    player.x,
                    player.z,
                    player.z,
                    false
                )
            )

            isTeleported = true
            if (debugFly)
                Chat.print("Teleported.. Fly Now!")
        }
    }
}