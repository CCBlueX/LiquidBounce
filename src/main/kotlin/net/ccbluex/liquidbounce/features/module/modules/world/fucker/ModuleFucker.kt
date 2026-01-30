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
import net.ccbluex.fastutil.WeightedSortedList
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_DOWN
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getClosestSquaredDistanceTo
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import java.util.function.ToDoubleFunction
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : ClientModule("Fucker", ModuleCategories.WORLD, aliases = listOf("BedBreaker", "IdNuker")) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(range, it)
    }

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
    private val delay by int("Delay", 0, 0..20, "ticks")
    private val action by enumChoice("Action", DestroyAction.DESTROY).apply(::tagBy)
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val ignoreUsingItem by boolean("IgnoreUsingItem", true)
    private val prioritizeOverKillAura by boolean("PrioritizeOverKillAura", false)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)

    // Rotation
    private val rotations = tree(RotationsValueGroup(this))
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
        targetRenderer.clearSilently()
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (!ignoreOpenInventory && mc.screen is AbstractContainerScreen<*>) {
            return@handler
        }

        if (!ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        oldTarget = currentTarget
        updateCurrentTarget()
    }

    @Suppress("unused")
    private val breaker = tickHandler {
        if (!ignoreOpenInventory && mc.screen is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        // If we don't have any new target, and we had one before, stop breaking.
        if (oldTarget != null && currentTarget == null) {
            interaction.stopDestroyBlock()
            return@tickHandler
        } else if (oldTarget != currentTarget && delay > 0) {
            interaction.stopDestroyBlock()
            waitTicks(delay)
        }

        // Check if blink is enabled - if so, we don't want to do anything.
        if (ModuleBlink.running) {
            return@tickHandler
        }

        val destroyerTarget = currentTarget ?: return@tickHandler
        val currentRotation = RotationManager.serverRotation
        targetRenderer.addBlock(destroyerTarget.pos)

        if (ModulePacketMine.running && destroyerTarget.action == DestroyAction.DESTROY) {
            ModulePacketMine.setTarget(destroyerTarget.pos)
            return@tickHandler
        }

        // Check if we are already looking at the block
        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@tickHandler
        ) ?: return@tickHandler

        val raytracePos = rayTraceResult.blockPos

        // Check if the raytrace result includes a block, if not we don't want to deal with it.
        if (rayTraceResult.type != HitResult.Type.BLOCK ||
            raytracePos != destroyerTarget.pos || raytracePos.getState()!!.isNotBreakable(raytracePos)
        ) {
            return@tickHandler
        }

        // Use action should be used if the block is the same as the current target and the action is set to use.
        if (destroyerTarget.action == DestroyAction.USE) {
            if (interaction.useItemOn(player, InteractionHand.MAIN_HAND, rayTraceResult) == InteractionResult.SUCCESS) {
                player.swing(InteractionHand.MAIN_HAND)
            }

            waitTicks(delay)
        } else {
            doBreak(rayTraceResult, immediate = forceImmediateBreak)
        }
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (currentTarget != null && !ModulePacketMine.running) {
            event.cancelEvent()
        }
    }

    private fun updateCurrentTarget() {
        val possibleBlocks = searchPossibleTargetPositions()

        validateCurrentTarget(possibleBlocks)

        if (possibleBlocks.isEmpty()) {
            return
        }

        val range = range.toDouble()

        // Find direct targets first
        if (possibleBlocks.any { pos ->
            // If the block has an entrance, we should ignore the wall range and act as if we are breaking normally.
            val wallRange = if (FuckerEntrance.enabled && pos.hasEntrance) range else wallRange.toDouble()
            considerAsTarget(DestroyerTarget(pos, action, isTarget = true), range, wallRange) == true
        } || currentTarget != null) {
            return
        }

        // Surrounding / Entrance
        for (pos in possibleBlocks) {
            // Is there any block in the way?
            if (FuckerEntrance.enabled && FuckerEntrance.breakFree) {
                val weakBlock = pos.weakestNeighbor ?: continue
                considerAsTarget(DestroyerTarget(weakBlock, DestroyAction.DESTROY), range, range)
            } else if (surroundings) {
                updateSurroundings(pos)
            }
        }
    }

    private fun clearCurrentTarget() {
        interaction.stopDestroyBlock()

        currentTarget?.let { target ->
            targetRenderer.removeBlock(target.pos)
        }
        currentTarget = null
    }

    private fun searchPossibleTargetPositions(): List<BlockPos> {
        return player.eyePosition.searchBlocksInCuboid(range + 1) { pos, state ->
            when (val block = state.block) {
                !in targets -> false
                is BedBlock if isSelfBedMode.activeMode.isSelfBed(block, pos) -> false
                else -> true
            }
        }.toCollection(WeightedSortedList(upperBound = range.sq().toDouble()) { (pos, state) ->
            state.getShape(world, pos, CollisionContext.of(player))
                .move(pos)
                .getClosestSquaredDistanceTo(player.eyePosition)
        }).mapToArray { it.first }.unmodifiable()
    }

    private fun validateCurrentTarget(possibleBlocks: Collection<BlockPos>) {
        val possibleBlocks = possibleBlocks.let {
            if (it is Set || it.size <= 4) it else it.toHashSet() // for performance of contains
        }
        val currentTarget = currentTarget ?: return

        var removed = false
        if (currentTarget.pos !in possibleBlocks) {
            removed = true
        }
        if (currentTarget.isTarget && currentTarget.action != action) {
            removed = true
        }

        // Stick with the current target because it's still valid.
        val validationResult =
            considerAsTarget(currentTarget, range.toDouble(), wallRange.toDouble(), isCurrentTarget = true)

        if (validationResult == false) {
            removed = true
        }

        if (removed) {
            clearCurrentTarget()
        }
    }

    private fun traceWayToTarget(
        target: BlockPos,
        startPos: BlockPos,
    ): List<BlockPos> {
        val eyePos = player.eyePosition
        val visited = LongOpenHashSet()
        val result = mutableListOf<BlockPos>()
        val targetPoint = target.collisionShape.closestPointTo(eyePos).getOrNull() ?: return emptyList()

        fun trace0(currBlock: Long) {
            val pos = BlockPos.MutableBlockPos()
            for (direction in Direction.entries) {
                pos.set(currBlock).move(direction)
                if (pos == target || pos.asLong() in visited) {
                    continue
                }

                // Any of boxes raycast the line is not null -> need to break
                var rc: Vec3? = null
                pos.collisionShape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                    if (rc == null) {
                        rc = AABB.clip(minX, minY, minZ, maxX, maxY, maxZ, eyePos, targetPoint).getOrNull()
                    }
                }

                rc ?: continue

                result.add(pos.immutable())
                visited.add(pos.asLong())

                trace0(pos.asLong())
            }
        }
        trace0(startPos.asLong())

        return result
    }

    /**
     * @return true if it is the best target, false if it's invalid and null if it's not better than the current target
     */
    private fun considerAsTarget(
        target: DestroyerTarget,
        range: Double,
        throughWallsRange: Double,
        isCurrentTarget: Boolean = false
    ): Boolean? {
        val state = target.pos.getState()

        if (state == null || state.isAir) {
            return false
        }

        val raytrace = raytraceBlockRotation(
            player.eyePosition,
            target.pos,
            state,
            range = range,
            wallsRange = throughWallsRange
        ) ?: return false

        val currentTarget = currentTarget

        if (!isCurrentTarget && currentTarget != null && target <= currentTarget) {
            return null
        }

        if (!ModulePacketMine.running) {
            RotationManager.setRotationTarget(
                raytrace.rotation,
                considerInventory = !ignoreOpenInventory,
                valueGroup = rotations,
                if (prioritizeOverKillAura) Priority.IMPORTANT_FOR_USAGE_3 else Priority.IMPORTANT_FOR_USAGE_1,
                this@ModuleFucker
            )
        }

        clearCurrentTarget()
        ModuleFucker.currentTarget = target

        return true
    }

    private fun updateSurroundings(initialPosition: BlockPos) {
        val raytraceResult = world.clip(
            ClipContext(
                player.eyePosition,
                initialPosition.center,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            )
        ) ?: return

        if (raytraceResult.type != HitResult.Type.BLOCK) {
            return
        }

        val blockPos = raytraceResult.blockPos

        val arr = traceWayToTarget(initialPosition, blockPos)

        val resistance = arr.mapNotNull {
            it to (it.getState()?.takeUnless { state -> state.isAir } ?: return@mapNotNull null)
        }.sumOf(::miningDuration)

        considerAsTarget(
            DestroyerTarget(blockPos, DestroyAction.DESTROY, SurroundingInfo(initialPosition, resistance)),
            range.toDouble(),
            wallRange.toDouble(),
        )
    }

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

    private enum class DestroyAction(override val tag: String) : Tagged {
        DESTROY("Destroy"), USE("Use")
    }

    private val BlockPos.hasEntrance: Boolean
        get() {
            val block = this.getBlock()
            val cache = BlockPos.MutableBlockPos()
            return DIRECTIONS_EXCLUDING_DOWN.any {
                val neighbor = cache.setWithOffset(this, it)
                neighbor.collisionShape == Shapes.empty() && neighbor.getBlock() !== block
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
                .getClosestSquaredDistanceTo(player.eyePosition)
        })

    @JvmStatic
    private fun miningDuration(pair: Pair<BlockPos, BlockState>): Double {
        val (pos, state) = pair
        val bestMiningSpeed = availableToolSlots.maxOfOrNull { it.itemStack.getDestroySpeed(state) } ?: 1.0F
        return state.getDestroySpeed(world, pos).toDouble() / bestMiningSpeed.toDouble()
    }

}
