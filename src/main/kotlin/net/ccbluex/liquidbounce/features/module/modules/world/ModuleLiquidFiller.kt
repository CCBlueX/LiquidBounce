/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.hasAnySolidPlacementNeighbor
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BucketPickup
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.SpongeBlock
import net.minecraft.world.phys.Vec3
import kotlin.math.max

/**
 * LiquidFiller module
 *
 * Places blocks inside of liquid source blocks within range of you.
 */
object ModuleLiquidFiller : ClientModule("LiquidFiller", ModuleCategories.WORLD) {

    private val placeIn by multiEnumChoice("PlaceIn", enumSetAllOf<PlaceIn>(), canBeNone = false)
    private val placeOrder by enumChoice("PlaceOrder", PlaceOrder.FURTHER_FIRST)
    private val useSponge by boolean("UseSponge", false)

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val blocks by blocks(
        "Blocks",
        findBlocksEndingWith("WOOL").apply {
            add(Blocks.DIRT)
            add(Blocks.COBBLESTONE)
            add(Blocks.STONE)
            add(Blocks.NETHERRACK)
            add(Blocks.DIORITE)
            add(Blocks.GRANITE)
            add(Blocks.ANDESITE)
        }
    )

    private val placer = tree(BlockPlacer(
        "Placer",
        this,
        Priority.NORMAL,
        ::findSlotForTarget,
        allowSupportPlacements = false
    ))

    override fun onDisabled() {
        placer.disable()
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (findSlotForTarget(null) == null) {
            if (!placer.isDone()) {
                placer.clear()
            }
            return@handler
        }

        placer.update(findFillTargets())
    }

    private fun findFillTargets(): List<BlockPos> {
        val eyePos = player.eyePosition
        val scanRange = max(placer.range, placer.wallRange).toDouble()
        val rangeSq = scanRange.sq()
        val normalFillSlot = filter.getSlot(blocks)
        val spongeSlot = spongeSlot()

        val positions = eyePos.searchBlocksInCuboid(scanRange.toFloat()) { pos, state ->
            pos.distToCenterSqr(eyePos) <= rangeSq && shouldFill(state, normalFillSlot != null, spongeSlot != null)
        }.mapTo(mutableListOf()) { (pos, _) -> pos }
        placeOrder.sort(positions, eyePos)

        return positions.mapNotNull { target ->
            if (useSponge && isWaterTarget(target)) {
                findSpongePlacement(target, scanRange)
            } else {
                target
            }
        }
    }

    private fun shouldFill(state: BlockState, hasNormalFiller: Boolean, hasSponge: Boolean): Boolean {
        if (!state.canBeReplaced()) {
            return false
        }

        val fluidState = state.fluidState
        if (fluidState.isEmpty || !fluidState.isSource) {
            return false
        }

        return when {
            PlaceIn.WATER in placeIn && fluidState.`is`(FluidTags.WATER) ->
                if (useSponge) hasSponge else hasNormalFiller

            PlaceIn.LAVA in placeIn && fluidState.`is`(FluidTags.LAVA) -> hasNormalFiller
            else -> false
        }
    }

    private fun findSlotForTarget(pos: BlockPos?): HotbarItemSlot? {
        val spongeSlot = spongeSlot()
        val normalFillSlot = filter.getSlot(blocks)

        if (pos == null) {
            return spongeSlot ?: normalFillSlot
        }

        return when {
            useSponge && isWaterTarget(pos) -> spongeSlot
            else -> normalFillSlot
        }
    }

    private fun isWaterTarget(pos: BlockPos) = world.getBlockState(pos).fluidState.`is`(FluidTags.WATER)

    private fun findSpongePlacement(waterPos: BlockPos, scanRange: Double): BlockPos? {
        return waterPos.center.searchBlocksInCuboid(SpongeBlock.MAX_DEPTH.toFloat()) { pos, state ->
            pos.distToCenterSqr(player.eyePosition) <= scanRange.sq() &&
                state.canBeReplaced() &&
                pos.hasAnySolidPlacementNeighbor() &&
                !pos.isBlockedByEntities() &&
                canAbsorbWaterFrom(pos, waterPos)
        }.minByOrNull { (pos, _) -> pos.distToCenterSqr(player.eyePosition) }?.first
    }

    /**
     * @see SpongeBlock.removeWaterBreadthFirstSearch
     */
    @Suppress("CognitiveComplexMethod")
    private fun canAbsorbWaterFrom(spongePos: BlockPos, waterPos: BlockPos): Boolean {
        if (spongePos == waterPos) {
            return true
        }

        var reachedTarget = false
        BlockPos.breadthFirstTraversal(
            spongePos,
            SpongeBlock.MAX_DEPTH,
            SpongeBlock.MAX_COUNT + 1,
            { pos, consumer ->
                for (direction in Direction.entries) {
                    consumer.accept(pos.relative(direction))
                }
            },
            { pos ->
                if (pos == spongePos) {
                    return@breadthFirstTraversal BlockPos.TraversalNodeStatus.ACCEPT
                }

                val state = pos.state ?: return@breadthFirstTraversal BlockPos.TraversalNodeStatus.SKIP
                val fluidState = state.fluidState
                if (!fluidState.`is`(FluidTags.WATER)) {
                    return@breadthFirstTraversal BlockPos.TraversalNodeStatus.SKIP
                }

                if (pos == waterPos) {
                    reachedTarget = true
                    return@breadthFirstTraversal BlockPos.TraversalNodeStatus.STOP
                }

                when (val block = state.block) {
                    is BucketPickup -> BlockPos.TraversalNodeStatus.ACCEPT

                    else -> if (
                        block === Blocks.KELP ||
                        block === Blocks.KELP_PLANT ||
                        block === Blocks.SEAGRASS ||
                        block === Blocks.TALL_SEAGRASS
                    ) {
                        BlockPos.TraversalNodeStatus.ACCEPT
                    } else {
                        BlockPos.TraversalNodeStatus.SKIP
                    }
                }
            }
        )

        return reachedTarget
    }

    private fun spongeSlot() = if (useSponge) {
        Slots.OffhandWithHotbar.findClosestSlot(Items.SPONGE)
    } else {
        null
    }

    private enum class PlaceIn(override val tag: String) : Tagged {
        WATER("Water"),
        LAVA("Lava")
    }

    private enum class PlaceOrder(override val tag: String) : Tagged {
        RANDOM("Random") {
            override fun sort(positions: MutableList<BlockPos>, eyePos: Vec3) {
                positions.shuffle()
            }
        },

        CLOSER_FIRST("CloserFirst") {
            override fun sort(positions: MutableList<BlockPos>, eyePos: Vec3) {
                positions.sortBy { it.distToCenterSqr(eyePos) }
            }
        },

        FURTHER_FIRST("FurtherFirst") {
            override fun sort(positions: MutableList<BlockPos>, eyePos: Vec3) {
                positions.sortByDescending { it.distToCenterSqr(eyePos) }
            }
        },

        BOTTOM_TOP("BottomTop") {
            override fun sort(positions: MutableList<BlockPos>, eyePos: Vec3) {
                positions.sortBy { it.y }
            }
        },

        TOP_BOTTOM("TopBottom") {
            override fun sort(positions: MutableList<BlockPos>, eyePos: Vec3) {
                positions.sortByDescending { it.y }
            }
        };

        abstract fun sort(positions: MutableList<BlockPos>, eyePos: Vec3)
    }
}
