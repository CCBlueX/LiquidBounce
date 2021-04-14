/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

/**
 * ChestAura module
 *
 * Automatically opens chests around you.
 */
object ModuleChestAura : Module("ChestAura", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val delay by int("Delay", 5, 1..80)
    private val visualSwing by boolean("VisualSwing", true)
    private val chest by blocks("Chest", mutableListOf(Blocks.CHEST))
    private val throughWalls by boolean("ThroughWalls", false)

    private object AwaitContainerOptions : ToggleableConfigurable(this, "AwaitContainer", true) {
        val timeout by int("Timeout", 10, 1..80)
        val maxRetrys by int("MaxRetries", 4, 1..10)
    }

    init {
        tree(AwaitContainerOptions)
    }

    // Rotation
    private val rotations = RotationsConfigurable()

    private var currentBlock: BlockPos? = null
    val clickedBlocks = hashSetOf<BlockPos>()

    var currentRetries = 0

    val networkTickHandler = repeatable { event ->
        if (mc.currentScreen != null) {
            return@repeatable
        }

        if (mc.currentScreen is HandledScreen<*>) {
            wait { delay }
        }

        updateTarget()

        val curr = currentBlock ?: return@repeatable
        val serverRotation = RotationManager.serverRotation ?: return@repeatable

        val rayTraceResult = raytraceBlock(
            range.toDouble(),
            serverRotation,
            curr,
            curr.getState() ?: return@repeatable
        )

        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr) {
            return@repeatable
        }

        if (interaction.interactBlock(
                player,
                mc.world!!,
                Hand.MAIN_HAND,
                rayTraceResult
            ) == ActionResult.SUCCESS
        ) {
            if (visualSwing) {
                player.swingHand(Hand.MAIN_HAND)
            } else {
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }

            var success = false

            if (AwaitContainerOptions.enabled) {
                wait {
                    if (mc.currentScreen is HandledScreen<*>) {
                        success = true

                        0
                    } else {
                        AwaitContainerOptions.timeout
                    }
                }
            } else {
                clickedBlocks.add(currentBlock!!)
                currentBlock = null
                success = true

                wait { delay }
            }

            if (success || currentRetries >= AwaitContainerOptions.maxRetrys) {
                clickedBlocks.add(currentBlock!!)
                currentBlock = null
            } else {
                currentRetries++
            }
        }

    }

    private fun updateTarget() {
        val targetedBlocks = hashSetOf<Block>()

        targetedBlocks.addAll(chest)

        val radius = range + 1
        val radiusSquared = radius * radius
        val eyesPos = mc.player!!.eyesPos

        val blocksToProcess = searchBlocks(radius.toInt()) { pos, state ->
            targetedBlocks.contains(state.block) && pos !in clickedBlocks && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        var nextBlock: BlockPos? = null

        for ((pos, state) in blocksToProcess) {
            val (rotation, _) = RotationManager.raytraceBlock(
                player.eyesPos,
                pos,
                state,
                throughWalls = throughWalls,
                range = range.toDouble()
            ) ?: continue

            // aim on target
            RotationManager.aimAt(rotation, configurable = rotations)
            nextBlock = pos
            break
        }

        if (currentBlock != nextBlock) {
            currentRetries = 0
        }

        currentBlock = nextBlock
    }

    override fun disable() {
        clickedBlocks.clear()
    }

}
