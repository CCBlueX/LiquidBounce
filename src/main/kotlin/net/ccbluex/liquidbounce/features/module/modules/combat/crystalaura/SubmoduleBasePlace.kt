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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementPositionCandidate
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Tries to build improved placement spots.
 */
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
         * Exclude y levels directly beneath simulated positions to not create crystal platforms for enemies.
         */
        val antiPlatform by boolean("AntiPlatformSimulate", true)

    }

    init {
        tree(SimulateMovement)
    }

    val minAdvantage by float("MinAdvantage", 2.0f, 0.1f..10f)

    private fun findBaseSlot(): HotbarItemSlot? =
        Slots.OffhandWithHotbar.findSlot(Items.OBSIDIAN) ?: Slots.OffhandWithHotbar.findSlot(Items.BEDROCK)

    private val placer = tree(BlockPlacer(
        "Placing",
        this,
        Priority.IMPORTANT_FOR_USAGE_2,
        slotFinder = { _ -> findBaseSlot() },
    ))

    @Volatile
    var currentTarget: PlacementPositionCandidate? = null
        set(value) {
            field = value
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
            } ?: run { placer.clear() }
        }

    private val calculations = Chronometer()
    private val trying = Chronometer()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val active = currentTarget ?: return@handler
        val block = active.pos.stateOrEmpty.block
        val alreadyPlaced = block == Blocks.OBSIDIAN || block == Blocks.BEDROCK
        if (alreadyPlaced || !hasAvailableBlocks || trying.hasElapsed(timeOut.toLong())) {
            placer.clear()
            currentTarget = null
        }
    }

    override fun onDisabled() {
        placer.disable()
        currentTarget = null
    }

    val hasAvailableBlocks: Boolean
        get() = enabled && placer.canAct() && findBaseSlot() != null

    /**
     * Returns whether base place should be calculated.
     */
    fun shouldBasePlaceRun(): Boolean {
        if (!hasAvailableBlocks) {
            if (currentTarget != null) {
                mc.execute { currentTarget = null }
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
     * Thread-safe check ensuring [pos] is anchored to an existing block directly or within support depth,
     * preventing unreachable floating mid-air blocks from starving crystal placements (#8053, #8140).
     */
    fun hasFoundation(pos: BlockPos): Boolean {
        if (!hasAvailableBlocks) {
            return false
        }

        if (Direction.entries.any { dir -> !pos.relative(dir).stateOrEmpty.canBeReplaced() }) {
            return true
        }

        if (!placer.support.enabled || placer.support.filter.getSlot(placer.support.blocks) == null) {
            return false
        }

        val maxDist = placer.support.depth + 1
        return BlockPos.withinManhattan(pos, maxDist)
            .any { candidate -> candidate != pos && !candidate.stateOrEmpty.canBeReplaced() }
    }

    /**
     * Returns a set of y levels the base place can be placed in.
     */
    fun getBasePlaceLayers(targetY: Double): IntRange {
        var down = 3
        var maxY = if (targetY - floor(targetY) > 0.2) {
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
        val maxBaseRange = min(
            max(placer.range, placer.wallRange),
            min(SubmoduleCrystalPlacer.getMaxRange(), SubmoduleCrystalDestroyer.getMaxRange())
        )
        return running &&
            (!onlyAboveSelf || pos.y >= player.blockPosition().y) &&
            pos.y in layers &&
            pos.getCenterDistanceSquared() < (maxBaseRange + 0.5f).sq() &&
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
        val halfWidth = player.bbWidth * 0.5
        val height = player.bbHeight.toDouble()
        val errorStep = SimulateMovement.errorStep.toDouble()
        val errorDown = SimulateMovement.downError
        var errorOffset = SimulateMovement.error.toDouble()

        // check if the pos will intersect at any expected position
        return snapShots.none {
            val simulatedPos = it.pos
            val error = errorOffset
            val minX = simulatedPos.x - halfWidth - error
            val maxX = simulatedPos.x + halfWidth + error
            val minZ = simulatedPos.z - halfWidth - error
            val maxZ = simulatedPos.z + halfWidth + error
            val result = posBB.intersects(
                minX,
                simulatedPos.y - if (errorDown) error else 0.0,
                minZ,
                maxX,
                simulatedPos.y + height + error,
                maxZ
            )
            errorOffset += errorStep
            // A base at y puts the crystal at y + 1, acting as a platform at our feet when floor(y) == y + 1
            val isPlatform = SimulateMovement.antiPlatform && floor(simulatedPos.y) - 1.0 == y
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
        // Do we find an escape side?
        layerA.forEachIndexed { index, pos1 ->
            if (!pos1.stateOrEmpty.isSolid && !layerB[index].stateOrEmpty.isSolid) {
                return true
            }
        }

        return false
    }

}
