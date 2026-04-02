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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.fucker

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.breaker.BlockBreaker
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_DOWN
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.math.distanceToSqr
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRangeSorted
import net.ccbluex.liquidbounce.utils.block.outlineShape
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.math.clipAllBoxes
import net.ccbluex.liquidbounce.utils.raytracing.clip
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.function.ToDoubleFunction
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : ClientModule("Fucker", ModuleCategories.WORLD, aliases = listOf("BedBreaker", "IdNuker")) {

    /**
     * Entrance requires the target block to have an entrance. It does not matter if we can see it or not.
     * If this condition is true, it will override the wall range to range
     * and act as if we were breaking normally.
     *
     * Useful for Hypixel and CubeCraft
     */
    private object FuckerEntrance : ToggleableValueGroup(this, "Entrance", false) {
        /**
         * Breaks the weakest block around target block and makes an entrance
         */
        val breakFree by boolean("BreakFree", true)
    }

    init {
        tree(FuckerEntrance)
    }

    private val surroundings by boolean("Surroundings", true)
    private val targets by blocks("Targets", findBlocksEndingWith("_BED", "DRAGON_EGG"))
    private val action by enumChoice("Action", DestroyAction.DESTROY).apply(::tagBy)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)
    private val blockBreaker = tree(BlockBreaker("Breaker", this))
    private val targetRenderer = tree(
        PlacementRenderer("TargetRendering", true, this,
            defaultColor = Color4b(255, 0, 0, 90)
        )
    )

    private val availableToolSlots
        get() = if (ModuleAutoTool.isInventoryConsidered) Slots.Hotbar + Slots.Inventory else Slots.Hotbar

    private var currentTarget: DestroyerTarget? = null
    private var oldTarget: DestroyerTarget? = null

    override fun onDisabled() {
        clearCurrentTarget()
        oldTarget = null
        blockBreaker.disable()
        targetRenderer.clearSilently()
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (blockBreaker.isBlocked()) {
            return@handler
        }

        oldTarget = currentTarget
        updateCurrentTarget()

        if (oldTarget?.pos != currentTarget?.pos) {
            oldTarget?.let { targetRenderer.removeBlock(it.pos) }
            currentTarget?.let { targetRenderer.addBlock(it.pos) }
        }
    }

    @Suppress("unused")
    private val useHandler = tickHandler {
        if (blockBreaker.isBlocked()) {
            return@tickHandler
        }

        val destroyerTarget = currentTarget ?: return@tickHandler
        if (destroyerTarget.action != DestroyAction.USE) {
            return@tickHandler
        }

        if (oldTarget != currentTarget && blockBreaker.switchDelay > 0) {
            waitTicks(blockBreaker.switchDelay)
            if (currentTarget != destroyerTarget) {
                return@tickHandler
            }
        }

        val currentRotation = RotationManager.serverRotation

        val rayTraceResult = raytraceBlock(
            max(blockBreaker.range, blockBreaker.wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@tickHandler
        ) ?: return@tickHandler

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != destroyerTarget.pos) {
            return@tickHandler
        }

        if (interaction.useItemOn(player, InteractionHand.MAIN_HAND, rayTraceResult) == InteractionResult.SUCCESS) {
            player.swing(InteractionHand.MAIN_HAND)
        }

        waitTicks(blockBreaker.switchDelay)
    }

    private fun updateCurrentTarget() {
        val possibleBlocks = searchPossibleTargetPositions()
        val selection = selectTarget(possibleBlocks)

        currentTarget = selection?.target
        blockBreaker.setTarget(selection?.takeIf { it.target.action == DestroyAction.DESTROY }?.preparedTarget)
    }

    private fun clearCurrentTarget() {
        currentTarget?.let { target ->
            targetRenderer.removeBlock(target.pos)
        }
        currentTarget = null
        blockBreaker.clear()
    }

    private fun searchPossibleTargetPositions(): List<BlockPos> {
        return player.eyePosition.searchBlocksInRangeSorted(blockBreaker.range) { pos, state ->
            when (val block = state.block) {
                !in targets -> false
                is BedBlock if isSelfBedMode.activeMode.isSelfBed(block, pos) -> false
                else -> true
            }
        }.map { it.first }
    }

    @Suppress("ReturnCount")
    private fun selectTarget(possibleBlocks: List<BlockPos>): TargetSelection? {
        validateCurrentTarget(possibleBlocks)?.let { return it }

        val range = blockBreaker.range.toDouble()

        for (pos in possibleBlocks) {
            val wallRange = if (FuckerEntrance.enabled && pos.hasEntrance) range else blockBreaker.wallRange.toDouble()
            val preparedTarget = blockBreaker.prepareTarget(pos, wallRange) ?: continue
            return TargetSelection(DestroyerTarget(pos, action, isTarget = true), preparedTarget)
        }

        for (pos in possibleBlocks) {
            if (FuckerEntrance.enabled && FuckerEntrance.breakFree) {
                val weakBlock = pos.weakestNeighbor ?: continue
                val preparedTarget = blockBreaker.prepareTarget(weakBlock, range) ?: continue
                return TargetSelection(DestroyerTarget(weakBlock, DestroyAction.DESTROY), preparedTarget)
            }

            if (surroundings) {
                selectSurroundingTarget(pos)?.let { return it }
            }
        }

        return null
    }

    private fun validateCurrentTarget(possibleBlocks: Collection<BlockPos>): TargetSelection? {
        val currentTarget = currentTarget ?: return null
        if (currentTarget.pos !in possibleBlocks) {
            return null
        }
        if (currentTarget.isTarget && currentTarget.action != action) {
            return null
        }

        val preparedTarget = blockBreaker.prepareTarget(currentTarget.pos) ?: return null
        return TargetSelection(currentTarget, preparedTarget)
    }

    private fun traceWayToTarget(
        target: BlockPos,
        targetPoint: Vec3,
        startPos: BlockPos,
    ): List<BlockPos> {
        val eyePos = player.eyePosition

        val visited = LongOpenHashSet()
        val result = mutableListOf<BlockPos>()

        fun traceSingle(pos: BlockPos): Boolean {
            if (pos == target || !visited.add(pos.asLong())) {
                return false
            }

            // Any of boxes raycast the line is not null -> need to break
            return pos.outlineShape.clipAllBoxes(pos, eyePos, targetPoint).isNotEmpty()
        }

        fun traceSurrounding(pos: Long) {
            val mut = BlockPos.MutableBlockPos().set(pos)
            if (traceSingle(mut)) {
                result.add(mut.immutable())
            }

            for (direction in Direction.entries) {
                mut.set(pos).move(direction)
                if (traceSingle(mut)) {
                    result.add(mut.immutable())
                    traceSurrounding(mut.asLong())
                }
            }
        }

        traceSurrounding(startPos.asLong())

        return result
    }

    @Suppress("ReturnCount")
    private fun selectSurroundingTarget(initialPosition: BlockPos): TargetSelection? {
        val eyePos = player.eyePosition
        val targetPoint = initialPosition.outlineShape.move(initialPosition)
            .closestPointTo(eyePos).getOrNull() ?: return null

        debugGeometry("targetPos") {
            ModuleDebug.DebuggedPoint(targetPoint, Color4b.RED.alpha(100))
        }

        val raytraceResult = world.clip(
            eyePos,
            targetPoint,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player,
        ).takeIf { it.type == HitResult.Type.BLOCK } ?: return null

        val blockPos = raytraceResult.blockPos

        debugGeometry("initialPosition") {
            ModuleDebug.DebuggedBox(initialPosition.outlineBox.move(initialPosition), Color4b.GREEN.alpha(50))
        }

        debugGeometry("raytraceResult") {
            ModuleDebug.DebuggedBox(blockPos.outlineBox.move(blockPos), Color4b.BLUE.alpha(50))
        }

        val arr = traceWayToTarget(initialPosition, targetPoint, blockPos).ifEmpty { return null }

        debugParameter("wayToTarget") { arr }

        val resistance = arr.mapNotNull {
            it to (it.getState()?.takeUnless { state -> state.isAir } ?: return@mapNotNull null)
        }.sumOf(::miningDuration)

        val preparedTarget = blockBreaker.prepareTarget(blockPos) ?: return null
        return TargetSelection(
            DestroyerTarget(blockPos, DestroyAction.DESTROY, SurroundingInfo(initialPosition, resistance)),
            preparedTarget
        )
    }

    @JvmRecord
    private data class DestroyerTarget(
        val pos: BlockPos,
        val action: DestroyAction,
        val surroundingInfo: SurroundingInfo? = null,
        val isTarget: Boolean = false
    ) : Comparable<DestroyerTarget> {
        override fun compareTo(other: DestroyerTarget): Int {
            val currentSurrounding = this.surroundingInfo
            val otherSurrounding = other.surroundingInfo

            return when {
                this.isTarget -> -1
                other.isTarget -> 1
                currentSurrounding == null -> -1
                otherSurrounding == null -> 1
                else -> currentSurrounding.resistance.compareTo(otherSurrounding.resistance)
            }
        }
    }

    /**
     * @param actualTargetPos the parent DestroyerTarget is surrounding this block
     * @param resistance proportional to the time it will take until the actual target is reached
     */
    private data class SurroundingInfo(
        val actualTargetPos: BlockPos,
        val resistance: Double
    )

    private data class TargetSelection(
        val target: DestroyerTarget,
        val preparedTarget: BlockBreaker.PreparedTarget
    )

    private enum class DestroyAction(override val tag: String) : Tagged {
        DESTROY("Destroy"), USE("Use")
    }

    private val BlockPos.hasEntrance: Boolean
        get() {
            val block = this.getBlock()
            val cache = BlockPos.MutableBlockPos()
            return DIRECTIONS_EXCLUDING_DOWN.any {
                val neighbor = cache.setWithOffset(this, it)
                neighbor.outlineShape.isEmpty && neighbor.getBlock() !== block
            }
        }

    private val BlockPos.weakestNeighbor: BlockPos?
        get() {
            val block = this.getBlock()
            val cache = BlockPos.MutableBlockPos()
            val neighbors = DIRECTIONS_EXCLUDING_DOWN.mapNotNullTo(mutableListOf()) {
                val neighbor = cache.setWithOffset(this, it)
                val state = neighbor.getState() ?: return@mapNotNullTo null
                if (state.block !== block && !state.isAir) neighbor.immutable() to state else null
            }

            return neighbors.minWithOrNull(comparator)?.first
        }

    private val comparator = Comparator.comparingDouble(ToDoubleFunction(::miningDuration))
        .thenComparingDouble(ToDoubleFunction { (pos, state) ->
            state.getShape(world, pos, CollisionContext.of(player))
                .move(pos)
                .distanceToSqr(player.eyePosition)
        })

    @JvmStatic
    private fun miningDuration(pair: Pair<BlockPos, BlockState>): Double {
        val (pos, state) = pair
        val bestMiningSpeed = availableToolSlots.maxOfOrNull { it.itemStack.getDestroySpeed(state) } ?: 1.0F
        return state.getDestroySpeed(world, pos).toDouble() / bestMiningSpeed.toDouble()
    }

}
