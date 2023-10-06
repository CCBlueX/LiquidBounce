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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
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
    private val wallRange by float("WallRange", 0f, 0F..6F).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }
    private val delay by int("Delay", 5, 1..80)
    private val visualSwing by boolean("VisualSwing", true)
    private val chest by blocks("Chest", hashSetOf(Blocks.CHEST))

    private object AwaitContainerOptions : ToggleableConfigurable(this, "AwaitContainer", true) {
        val timeout by int("Timeout", 10, 1..80)
        val maxRetrys by int("MaxRetries", 4, 1..10)
    }

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private object CloseInstantlyOptions :
        ToggleableConfigurable(this, "CloseInstantly", false) { // FIXME: Close instantly
        val timeout by int("Timeout", 2500, 100..10000)
    }

    init {
        tree(AwaitContainerOptions)
        tree(CloseInstantlyOptions)
    }

    private val closeInstantlyTimeout = Chronometer()

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private var currentBlock: BlockPos? = null
    val clickedBlocks = hashSetOf<BlockPos>()

    var currentRetries = 0

    val networkTickHandler = repeatable { event ->
//        if (mc.currentScreen is HandledScreen<*>) {
//            if (CloseInstantlyOptions.enabled && !closeInstantlyTimeout.hasElapsed(CloseInstantlyOptions.timeout.toLong())) {
//                player.closeHandledScreen()
//            }
//
//            wait { delay }
//        }

        if (mc.currentScreen != null) {
            return@repeatable
        }

        updateTarget()

        val curr = currentBlock ?: return@repeatable
        val currentRotation = RotationManager.currentRotation ?: return@repeatable

        val rayTraceResult = raytraceBlock(
            range.toDouble(),
            currentRotation,
            curr,
            curr.getState() ?: return@repeatable
        )

        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr) {
            return@repeatable
        }

        if (interaction.interactBlock(
                player,
                Hand.MAIN_HAND,
                rayTraceResult
            ) == ActionResult.SUCCESS
        ) {
            closeInstantlyTimeout.reset()

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
                clickedBlocks.add(curr)
                currentBlock = null
                success = true

                wait { delay }
            }

            if (success || currentRetries >= AwaitContainerOptions.maxRetrys) {
                clickedBlocks.add(curr)
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
        val eyesPos = mc.player!!.eyes

        val blocksToProcess = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            targetedBlocks.contains(state.block) && pos !in clickedBlocks && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        var nextBlock: BlockPos? = null

        for ((pos, state) in blocksToProcess) {
            val (rotation, _) = raytraceBlock(
                player.eyes,
                pos,
                state,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble()
            ) ?: continue

            // aim on target
            RotationManager.aimAt(rotation, openInventory = ignoreOpenInventory, configurable = rotations)
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
