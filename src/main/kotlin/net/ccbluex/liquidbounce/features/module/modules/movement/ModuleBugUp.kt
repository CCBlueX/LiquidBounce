/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.max

/**
 * BugUp module
 *
 * Automatically setbacks you after falling a certain distance.
 */

object ModuleBugUp : Module("BugUp", Category.MOVEMENT) {

    private val mode by enumChoice("Mode", Mode.BLINK_BACK)

    private val maximumFallDamage by float("MaximumFallDamage", 3F, 0F..40F)

    private var fallingLocation: BlockPos? = null
    private var previousPosition: Vec3d? = null
    private var actionTaken = false

    var takeAction = false

    val shouldLag
        get() = enabled && mode == Mode.BLINK_BACK && takeAction

    val isAtASafePlace
        get() = player.isOnGround || player.blockPos.down().canStandOn()
            || player.isHoldingOntoLadder || player.isTouchingWater || player.velocity.y > 0.0

    val movementInputEvent = handler<MovementInputEvent> {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)
        )

        // Tick three times to make sure we are not falling
        for (i in 0 until 5) {
            simulatedPlayer.tick()

            if (simulatedPlayer.fallDistance > 0 && !simulatedPlayer.pos.toBlockPos().down().canStandOn()) {
                takeAction = true
                break
            }
        }
    }

    val listener = repeatable {
        fallingLocation = null

        if (isAtASafePlace) {
            previousPosition = player.prevPos
            actionTaken = false
            takeAction = false
        } else if (!actionTaken && player.fallDistance > 0.5) {
            val fallingPlayer = FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.velocity.x,
                player.velocity.y,
                player.velocity.z,
                player.yaw
            ).apply {
                fallingLocation = findCollision(60)?.pos
            }

            val totalFallDistance = player.fallDistance + (player.y - fallingPlayer.y)
            val fallDamage = max(0.0, totalFallDistance - 3)

            if (fallDamage > maximumFallDamage) {
                when (mode) {
                    Mode.TELEPORTBACK -> {
                        player.setPosition(previousPosition)
                        player.velocity = Vec3d.ZERO
                    }

                    Mode.FLYFLAG -> {
                        player.velocity.y += 0.1
                    }

                    Mode.ONGROUNDSPOOF -> network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true))

                    Mode.MOTIONTELEPORTFLAG -> {
                        player.updatePosition(player.x, player.y + 1f, player.z)
                        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true))
                        player.velocity.y = 0.1

                        player.strafe()
                    }

                    Mode.FREEZE -> {
                        enabled = true
                    }

                    Mode.BLINK_BACK -> {
                        val firstPositon = FakeLag.firstPosition() ?: return@repeatable
                        if (firstPositon.toBlockPos().canStandOn()) {
                            notification("BugUp", "No safe position to go back.",
                                NotificationEvent.Severity.ERROR)
                            actionTaken = true
                            return@repeatable
                        }

                        FakeLag.cancel()
                    }
                }

                notification("BugUp", "Action taken", NotificationEvent.Severity.INFO)
                actionTaken = true
            }
        }

    }

    val packetEvent = handler<PacketEvent> {

    }

    override fun disable() {
        previousPosition = null
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        FREEZE("Freeze"),
        TELEPORTBACK("TeleportBack"),
        FLYFLAG("FlyFlag"),
        ONGROUNDSPOOF("OnGroundSpoof"),
        MOTIONTELEPORTFLAG("MotionTeleport-Flag"),
        BLINK_BACK("BlinkBack")
    }

//    @EventTarget
//    fun onRender3D(event: Render3DEvent) {
//        val player = mc.player ?: return
//
//        if (detectedLocation == null || !indicator.get() ||
//                player.fallDistance + (player.posY - (detectedLocation!!.y + 1)) < 3)
//            return
//
//        val x = detectedLocation!!.x
//        val y = detectedLocation!!.y
//        val z = detectedLocation!!.z
//
//        val renderManager = mc.renderManager
//
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
//        GL11.glEnable(GL11.GL_BLEND)
//        GL11.glLineWidth(2f)
//        GL11.glDisable(GL11.GL_TEXTURE_2D)
//        GL11.glDisable(GL11.GL_DEPTH_TEST)
//        GL11.glDepthMask(false)
//
//        RenderUtils.glColor(Color(255, 0, 0, 90))
//        RenderUtils.drawFilledBox(classProvider.createAxisAlignedBB(
//                x - renderManager.renderPosX,
//                y + 1 - renderManager.renderPosY,
//                z - renderManager.renderPosZ,
//                x - renderManager.renderPosX + 1.0,
//                y + 1.2 - renderManager.renderPosY,
//                z - renderManager.renderPosZ + 1.0)
//        )
//
//        GL11.glEnable(GL11.GL_TEXTURE_2D)
//        GL11.glEnable(GL11.GL_DEPTH_TEST)
//        GL11.glDepthMask(true)
//        GL11.glDisable(GL11.GL_BLEND)
//
//        val fallDist = floor(player.fallDistance + (player.posY - (y + 0.5))).toInt()
//
//        RenderUtils.renderNameTag("${fallDist}m (~${max(0, fallDist - 3)} damage)", x + 0.5, y + 1.7, z + 0.5)
//
//        classProvider.getGlStateManager().resetColor()
//    }
}
