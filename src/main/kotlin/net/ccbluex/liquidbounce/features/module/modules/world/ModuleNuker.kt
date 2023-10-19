/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import com.viaversion.viaversion.util.ChatColorUtil
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.item.findBlocksEndingWith
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

/**
 * Nuker module
 *
 * Destroys blocks around you.
 */
object ModuleNuker : Module("Nuker", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }
    private val visualSwing by boolean("VisualSwing", true)

    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val switchDelay by int("SwitchDelay", 0, 0..20)

    private val comparisonMode by enumChoice("Preferred", ComparisonMode.CROSSHAIR, ComparisonMode.values())

    /**
     * Makes a safe platform
     */
    private object Platform : ToggleableConfigurable(this, "Platform", true) {

        val size by int("Size", 3, 0..5)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val base = Color4b(Color.GREEN)

            renderEnvironmentForWorld(matrixStack) {
                val playerPosition = player.blockPos.down()

                for (x in -size..size) {
                    for (z in -size..size) {
                        val vec3 = Vec3(playerPosition.x.toDouble() + x, playerPosition.y.toDouble(),
                            playerPosition.z.toDouble() + z)
                        val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

                        val baseColor = base.alpha(50)
                        val outlineColor = base.alpha(100)

                        withPosition(vec3) {
                            withColor(baseColor) {
                                drawSolidBox(box)
                            }

                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }

    }

    // Rotation behavior
    private val rotations = tree(RotationsConfigurable())


    private var currentTarget: DestroyerTarget? = null

    // TODO: Make use of path finding AI baritone
    //private object Prison : ToggleableConfigurable(this, "Prison", false) {

        //private val followArea by boolean("FollowArea", true)

    //}

    init {
        tree(Platform)
        //tree(Prison)
    }

    /**
     * Blacklist of blocks that are usual not meant to be broken
     */
    private val blacklistedBlocks = findBlocksEndingWith("BEDROCK").toHashSet()

    val repeat = repeatable {
        if (mc.currentScreen is HandledScreen<*>) {
            wait { switchDelay }
            return@repeatable
        }

        updateTarget()

        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val curr = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.serverRotation

        val rayTraceResult = raytraceBlock(
            range.toDouble() + 1, currentRotation, curr.pos, curr.pos.getState() ?: return@repeatable
        ) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr.pos) {
            return@repeatable
        }

        val blockPos = rayTraceResult.blockPos

        if (blockPos.getState()?.isAir == true) {
            return@repeatable
        }

        val direction = rayTraceResult.side

        if (forceImmediateBreak) {
            network.sendPacket(
                PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction)
            )
            swingHand()
            network.sendPacket(
                PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction)
            )
        } else {
            if (interaction.updateBlockBreakingProgress(blockPos, direction)) {
                swingHand()
            }
        }
    }

    private fun swingHand() {
        if (visualSwing) {
            player.swingHand(Hand.MAIN_HAND)
        } else {
            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        }
    }

    private fun updateTarget() {
        val radius = range
        val radiusSquared = radius * radius
        val eyesPos = player.eyes

        this.currentTarget = null

        val targets = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            !state.isAir && !blacklistedBlocks.contains(state.block) && !isOnPlatform(pos)
                && getNearestPoint(eyesPos, Box(pos, pos.add(1, 1, 1)))
                    .squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { (pos, state) ->
            when (comparisonMode) {
                ComparisonMode.CROSSHAIR -> RotationManager.rotationDifference(
                    RotationManager.makeRotation(pos.toCenterPos(), player.eyes),
                    RotationManager.serverRotation
                )
                ComparisonMode.DISTANCE -> pos.getCenterDistanceSquared()
                ComparisonMode.HARDNESS -> state.getHardness(world, pos).toDouble()
            }
        }

        if (targets.isEmpty()) {
            return
        }

        for ((pos, state) in targets) {
            val raytrace = raytraceBlock(player.eyes, pos, state, range = range.toDouble(),
                wallsRange = wallRange.toDouble())

            // Check if there is a free angle to the block.
            if (raytrace != null) {
                val (rotation, _) = raytrace
                RotationManager.aimAt(rotation, openInventory = ignoreOpenInventory, configurable = rotations)

                this.currentTarget = DestroyerTarget(pos, rotation)
                return
            }
        }
    }

    private fun isOnPlatform(block: BlockPos) = Platform.enabled
        && block.x <= player.blockPos.x + Platform.size && block.x >= player.blockPos.x - Platform.size
        && block.z <= player.blockPos.z + Platform.size && block.z >= player.blockPos.z - Platform.size
        && block.y == player.blockPos.down().y // Y level is the same as the player's feet

    data class DestroyerTarget(val pos: BlockPos, val rotation: Rotation)

    enum class ComparisonMode(override val choiceName: String) : NamedChoice {
        CROSSHAIR("Crosshair"), DISTANCE("Distance"), HARDNESS("Hardness")
    }

}
