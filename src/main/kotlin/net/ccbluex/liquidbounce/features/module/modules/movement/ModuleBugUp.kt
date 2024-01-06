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
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import kotlin.math.abs

/**
 * BugUp module
 *
 * Automatically setbacks you after falling a certain distance.
 */

object ModuleBugUp : Module("BugUp", Category.MOVEMENT) {
    private val maxFallDistance by int("MaxFallDistance", 10, 2..255)
    private val maxDistanceWithoutGround by float("MaxDistanceToSetback", 2.5f, 1f..30f)
    private val mode = enumChoice("Mode", Mode.FLYFLAG, Mode.values())
    //private val indicator by boolean("Indicator", true)

    private var detectedLocation: BlockPos? = null
    private var lastFound = 0F
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var actionTaken = false

    val listener = repeatable {
        detectedLocation = null

        if (ModuleBlink.enabled) {
            return@repeatable
        }


        if (player.isOnGround || !BlockPos(player.blockPos.down()).getState()!!.isAir) {
            prevX = player.prevX
            prevY = player.prevY
            prevZ = player.prevZ

            actionTaken = false
        }

        if (!player.isOnGround && !player.isHoldingOntoLadder && !player.isTouchingWater) {
            val fallingPlayer = FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.velocity.x,
                player.velocity.y,
                player.velocity.z,
                player.yaw
            )

            detectedLocation = fallingPlayer.findCollision(60)?.pos

            if (detectedLocation != null && abs(player.y - detectedLocation!!.y) +
                player.fallDistance <= maxFallDistance
            ) {
                lastFound = player.fallDistance
            }

            if (player.fallDistance - lastFound > maxDistanceWithoutGround && !actionTaken) {

                when (mode.value) {
                    Mode.TELEPORTBACK -> {
                        player.updatePosition(prevX, prevY, prevZ)
                        player.velocity.y = 0.0
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
                        ModuleFreeze.enabled = true
                    }
                }

                actionTaken = true
            }
        }

    }

    override fun disable() {
        prevX = 0.0
        prevY = 0.0
        prevZ = 0.0
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        FREEZE("Freeze"),
        TELEPORTBACK("TeleportBack"),
        FLYFLAG("FlyFlag"),
        ONGROUNDSPOOF("OnGroundSpoof"),
        MOTIONTELEPORTFLAG("MotionTeleport-Flag")
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
