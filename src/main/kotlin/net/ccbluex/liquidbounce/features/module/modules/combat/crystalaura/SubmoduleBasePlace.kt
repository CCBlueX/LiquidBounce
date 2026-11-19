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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.baseplace.findBasePlaceSupportPath
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.baseplace.selectBaseSlot
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementPositionCandidate
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.hasAnySolidPlacementNeighbor
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.isUnobstructed
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Tries to build improved placement spots.
 */
@Suppress("TooManyFunctions")
object SubmoduleBasePlace : ToggleableValueGroup(ModuleCrystalAura, "BasePlace", true) {

    /**
     * How long to wait before starting a new calculation.
     */
    private val delay by int("CalcDelay", 120, 0..1000, "ms")

    /**
     * After how many ms the placer get cleared.
     */
    private val timeOut by int("TimeOut", 240, 0..5000, "ms")

    /**
     * Only places below the enemy when this is true.
     */
    private val platformOnly by boolean("PlatformOnly", false)

    /**
     * Only places above our y level to not help the enemy.
     * This is only useful if the enemy is using crystals too, and we're fighting on normal terrain (not bedrock).
     */
    private val onlyAboveSelf by boolean("OnlyAboveSelf", false)

    /**
     * Excludes terrain for base place placements.
     * This can make the ca very inefficient in scuffed landscapes.
     *
     * Only has an effect if [CrystalAuraDamageOptions.terrain] is enabled.
     */
    val terrain by boolean("Terrain", true)

    /**
     * Makes sure we don't run into the placement. This does not mean the damage will be predicted at the simulated
     * position.
     */
    private object SimulateMovement : ToggleableValueGroup(this, "SimulateMovement", true) {

        /**
         * How many ticks the player movement is simulated.
         */
        val ticks by int("Ticks", 3, 1..20)

        /**
         * How much the players bounding box should grow with each simulated tick.
         */
        val error by float("Error", 0.1f, 0f..2f)

        /**
         * How much the error increases with each tick.
         */
        val errorStep by float("ErrorStep", 0.05f, 0f..1f)

        /**
         * Should the bounding box also extend downwards?
         */
        val downError by boolean("DownError", false)

        /**
         * Exclude y levels we might enter to not help enemies.
         */
        val antiPlatform by boolean("AntiPlatformSimulate", true)

    }

    init {
        tree(SimulateMovement)
    }

    val minAdvantage by float("MinAdvantage", 2.0f, 0.1f..10f)

    private fun findBaseSlot(): HotbarItemSlot? = selectBaseSlot(
        Slots.OffhandWithHotbar.findSlot(Items.OBSIDIAN),
        Slots.OffhandWithHotbar.findSlot(Items.BEDROCK),
    )

    private val placer = tree(BlockPlacer(
        "Placing",
        this,
        Priority.IMPORTANT_FOR_USAGE_2,
        slotFinder = { _ -> findBaseSlot() },
    ))

    @Volatile
    private var target: PlacementPositionCandidate? = null

    var currentTarget: PlacementPositionCandidate?
        get() = target
        set(value) {
            onClientThread {
                // A worker invalidated while its update was queued must not resurrect BasePlace after disable.
                if (value == null || running) {
                    updateCurrentTarget(value)
                }
            }
        }

    /** Must run on the client thread: BlockPlacer mutates FastUtil collections and renderers. */
    private fun updateCurrentTarget(value: PlacementPositionCandidate?) {
        target = value
        val blockedPositions = placer.support.blockedPositions
        blockedPositions.clear()
        value?.let {
            // make sure the placer won't build up the position
            blockedPositions.add(it.pos.above())
            if (SubmoduleCrystalPlacer.oldVersion) {
                blockedPositions.add(it.pos.above(2))
            }

            placer.update(setOf(it.pos))
            trying.reset()
        } ?: placer.clear()
    }

    private val calculations = Chronometer()
    private val trying = Chronometer()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val active = currentTarget ?: return@handler
        val state = active.pos.stateOrEmpty
        val alreadyPlaced = state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK
        val invalid = !alreadyPlaced && !state.canBeReplaced()
        if (alreadyPlaced || invalid || !hasAvailableBlocks || trying.hasElapsed(timeOut.toLong())) {
            currentTarget = null
        }
    }

    override fun onDisabled() {
        onClientThread {
            placer.disable()
            updateCurrentTarget(null)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        currentTarget = null
    }

    val hasAvailableBlocks: Boolean
        get() = onClientThread { running && placer.canAct() && findBaseSlot() != null } == true

    /**
     * Returns whether base place should be calculated.
     */
    fun shouldBasePlaceRun(): Boolean {
        if (!hasAvailableBlocks) {
            if (currentTarget != null) {
                currentTarget = null
            }
            return false
        }

        if (calculations.hasAtLeastElapsed(delay.toLong())) {
            calculations.reset()
            return true
        }

        return false
    }

    /**
     * Proves that [pos] can be placed now or reached by a connected support path.
     *
     * Candidate selection normally runs on CrystalAura's worker. The actual proof runs on the client thread because it
     * uses BlockPlacer's queues, support reservations, inventory and target finding. A missing path or an exhausted
     * search budget is rejected; proximity to arbitrary terrain is never accepted as a foundation.
     */
    fun hasFoundation(pos: BlockPos): Boolean {
        val target = pos.immutable
        return onClientThread { hasFoundationOnClient(target) } == true
    }

    private fun hasFoundationOnClient(pos: BlockPos): Boolean {
        if (!running || !placer.canAct() || findBaseSlot() == null) {
            return false
        }

        val support = placer.support
        val canSupport = support.enabled && support.filter.getSlot(support.blocks) != null
        if (!canSupport) {
            return placer.canClickPlace(pos)
        }

        val depth = support.depth
        val rangeSq = placer.range.sq()
        // BlockPlacer removes old support entries before searching. Keep those positions eligible for revalidation,
        // but never route a new path through another structural target.
        val queuedTargets = placer.blocks.long2BooleanEntrySet()
            .filterNot { entry -> entry.booleanValue }
            .map { entry -> entry.longKey }
            .toSet()
        return findBasePlaceSupportPath(
            target = pos,
            hasSolidNeighbor = { it.hasAnySolidPlacementNeighbor() },
            canClickPlace = placer::canClickPlace,
            neighbors = { current ->
                buildList {
                    for (direction in Direction.entries) {
                        val neighbor = current.relative(direction)
                        val invalidState = support.blockedPositions.contains(neighbor) ||
                            !neighbor.stateOrEmpty.canBeReplaced() ||
                            queuedTargets.contains(neighbor.asLong())
                        val outsideSearch = neighbor.distManhattan(pos) > depth ||
                            neighbor.distToCenterSqr(player.eyePosition) > rangeSq
                        if (invalidState || outsideSearch || !neighbor.isUnobstructed()) {
                            continue
                        }

                        add(neighbor)
                    }
                }
            },
            maxIterations = supportSearchIterationLimit(depth),
        ) != null
    }

    /** Six possible incoming edges per node plus the start, bounding queue expansions for this unit-cost search. */
    private fun supportSearchIterationLimit(depth: Int): Int {
        val nodes = 1 + 2 * depth * (depth + 1) * (2 * depth + 1) / 3
        return 6 * nodes + 1
    }

    /** Runs [block] on the client thread and waits for its result when called from CrystalAura's worker. */
    private fun <T> onClientThread(block: () -> T): T? {
        if (mc.isSameThread) {
            return block()
        }

        val future = CompletableFuture<T>()
        mc.execute {
            try {
                future.complete(block())
            } catch (throwable: Throwable) {
                future.completeExceptionally(throwable)
            }
        }

        return try {
            future.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    /**
     * Returns a set of y levels the  base place can be placed in.
     */
    fun getBasePlaceLayers(targetY: Double): IntRange {
        var down = 3
        var maxY = if (targetY % 1 > 0.2) {
            down++
            ceil(targetY).toInt()
        } else {
            floor(targetY).toInt()
        }

        if (platformOnly) {
            maxY--
            down--
        }

        return maxY - down + 1..maxY
    }

    /**
     * Returns `true` if we can place a crystal base at the [pos] currently.
     */
    fun canBasePlace(
        running: Boolean,
        pos: BlockPos,
        layers: IntRange,
        state: BlockState
    ): Boolean {
        return running &&
            (!onlyAboveSelf || pos.y >= player.blockPosition().y) &&
            pos.y in layers &&
            pos.getCenterDistanceSquared() + 1.0 < max(placer.range, placer.wallRange).sq() &&
            state.canBeReplaced() &&
            pos.isUnobstructed() &&
            playerWillNotRunIn(pos) &&
            willNotTrap(pos)
    }

    /**
     * Simulates player movement, so that we won't run into the position.
     */
    private fun playerWillNotRunIn(pos: BlockPos): Boolean {
        if (!SimulateMovement.enabled) {
            return true
        }

        val snapShots = PlayerSimulationCache
            .getSimulationForLocalPlayer()
            .getSnapshotsBetween(1..SimulateMovement.ticks)

        val posBB = FULL_BOX.move(pos)
        val y = pos.y.toDouble()
        val errorStep = SimulateMovement.errorStep.toDouble()
        val errorDown = SimulateMovement.downError
        var errorOffset = SimulateMovement.error.toDouble()

        // check if the pos will intersect at any expected position
        return snapShots.none {
            val simulatedPos = it.pos
            val error = errorOffset
            val result = posBB.intersects(
                simulatedPos.x - error,
                simulatedPos.y - if (errorDown) error else 0.0,
                simulatedPos.z - error,
                simulatedPos.x + 1.0 + error,
                simulatedPos.y + 1.8 + error,
                simulatedPos.z + 1.0 + error
            )
            errorOffset += errorStep
            val isPlatform = SimulateMovement.antiPlatform && floor(simulatedPos.y) == y
            val isBelow = onlyAboveSelf && y < simulatedPos.y
            result || isPlatform || isBelow
        }
    }

    /**
     * Checks if the position traps the player.
     */
    private fun willNotTrap(pos: BlockPos): Boolean {
        val bb = player.boundingBox
        val yA = ceil(bb.minY)
        val yB = floor(bb.maxY)

        val positions = arrayOf(
            DoubleDoubleImmutablePair(bb.minX, bb.minZ),
            DoubleDoubleImmutablePair(bb.minX, bb.maxZ),
            DoubleDoubleImmutablePair(bb.maxX, bb.minZ),
            DoubleDoubleImmutablePair(bb.maxX, bb.maxZ)
        )

        // the block layer below the player, if the player is clipped in the ground block a bit, it doesn't matter
        val floor = positions.mapToArray {
            BlockPos.containing(
                it.keyDouble(),
                yA - 1.0,
                it.valueDouble()
            )
        }

        // the first wall layer
        val layerA = positions.mapToArray {
            BlockPos.containing(
                it.keyDouble(),
                yA,
                it.valueDouble()
            )
        }

        // the second wall layer
        val layerB = positions.mapToArray {
            BlockPos.containing(
                it.keyDouble(),
                yB,
                it.valueDouble()
            )
        }

        // the blocks above the player
        val ceiling = positions.mapToArray {
            BlockPos.containing(
                it.keyDouble(),
                yB + 1.0,
                it.valueDouble()
            )
        }

        val isInFloorOrCeiling = pos in floor || pos in ceiling
        if (isInFloorOrCeiling) {
            return canEscapeThroughSides(layerA, layerB)
        }

        val isInWall = pos in layerA || pos in layerB
        if (isInWall) {
            return canEscapeThroughFloorOrCeiling(ceiling, floor)
        }

        return true
    }

    private fun canEscapeThroughFloorOrCeiling(
        ceiling: Array<BlockPos>,
        floor: Array<BlockPos>
    ): Boolean {
        // Can we escape through the ceiling?
        ceiling.forEach { pos1 ->
            if (!pos1.stateOrEmpty.isSolid && !pos1.above().stateOrEmpty.isSolid) {
                return true
            }
        }

        // Can we escape through the floor?
        // For example, with this floor:
        // o = air, x = a solid block
        // o x
        // x x
        // we could escape
        floor.forEach { pos1 ->
            if (!pos1.stateOrEmpty.isSolid && !pos1.below().stateOrEmpty.isSolid) {
                return true
            }
        }

        return false
    }

    private fun canEscapeThroughSides(layerA: Array<BlockPos>, layerB: Array<BlockPos>): Boolean {
        // Do we find and escape side?
        layerA.forEachIndexed { index, pos1 ->
            if (!pos1.stateOrEmpty.isSolid && !layerB[index].stateOrEmpty.isSolid) {
                return true
            }
        }

        return false
    }

}
