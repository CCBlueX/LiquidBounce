/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementPositionCandidate
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.minecraft.block.BlockState
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Tries to build improved placement spots.
 */
object SubmoduleBasePlace : ToggleableConfigurable(ModuleCrystalAura, "BasePlace", true) {

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
    private object SimulateMovement : ToggleableConfigurable(this, "SimulateMovement", true) {

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

    private val placer = tree(BlockPlacer(
        "Placing",
        ModuleCrystalAura,
        Priority.IMPORTANT_FOR_USAGE_2,
        slotFinder = { _ ->
            Slots.OffhandWithHotbar.findSlot(Items.OBSIDIAN) ?: Slots.OffhandWithHotbar.findSlot(Items.BEDROCK)
        }
    ))

    var currentTarget: PlacementPositionCandidate? = null
        set(value) {
            field = value
            val blockedPositions = placer.support.blockedPositions
            blockedPositions.clear()
            value?.let {
                // make sure the placer won't build up the position
                blockedPositions.add(it.pos.up())
                if (SubmoduleCrystalPlacer.oldVersion) {
                    blockedPositions.add(it.pos.up(2))
                }

                placer.update(setOf(it.pos))
                trying.reset()
            } ?: run { placer.clear() }
        }

    private val calculations = Chronometer()
    private val trying = Chronometer()

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (currentTarget != null && trying.hasElapsed(timeOut.toLong())) {
            placer.clear()
            currentTarget = null
        }
    }

    override fun onDisabled() {
        placer.disable()
        currentTarget = null
    }

    /**
     * Returns whether base place should be calculated.
     */
    fun shouldBasePlaceRun(): Boolean {
        if (!enabled) {
            return false
        }

        if (calculations.hasAtLeastElapsed(delay.toLong())) {
            calculations.reset()
            return true
        }

        return false
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
            (!onlyAboveSelf || pos.y >= player.blockPos.y) &&
            pos.y in layers &&
            pos.getCenterDistanceSquared() + 1.0 < max(placer.range, placer.wallRange) &&
            state.isReplaceable &&
            !pos.isBlockedByEntities() &&
            playerWillNotRunIn(pos) &&
            willNotTrap(pos)
    }

    /**
     * Simulates player movement, so that we won't run into the position.
     */
    private fun playerWillNotRunIn(pos: BlockPos): Boolean {
        if (SimulateMovement.enabled) {
            return true
        }

        val snapShots = PlayerSimulationCache
            .getSimulationForLocalPlayer()
            .getSnapshotsBetween(1..SimulateMovement.ticks)

        val posBB = FULL_BOX.offset(pos)
        val y = pos.y.toDouble()
        val errorStep = SimulateMovement.errorStep.toDouble()
        val errorDown = SimulateMovement.downError
        var errorOffset = SimulateMovement.error.toDouble()

        // check if the pos will intersect at any expected position
        return snapShots.none {
            val simulatedPos = it.pos
            val result = posBB.intersects(
                simulatedPos.x - errorStep,
                simulatedPos.y - if (errorDown) errorOffset else 0.0,
                simulatedPos.z - errorStep,
                simulatedPos.x + 1.0 + errorStep,
                simulatedPos.y + 1.8 + errorStep,
                simulatedPos.z + 1.0 + errorStep
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
        val yB = floor(bb.maxX)

        val positions = arrayOf(
            DoubleDoubleImmutablePair(bb.minX, bb.minZ),
            DoubleDoubleImmutablePair(bb.minX, bb.maxZ),
            DoubleDoubleImmutablePair(bb.maxX, bb.minZ),
            DoubleDoubleImmutablePair(bb.maxX, bb.maxZ)
        )

        // the block layer below the player, if the player is clipped in the ground block a bit, it doesn't matter
        val floor = positions.mapArray { BlockPos.ofFloored(it.keyDouble(), yA - 1.0, it.valueDouble()) }

        // the first wall layer
        val layerA = positions.mapArray { BlockPos.ofFloored(it.keyDouble(), yA, it.valueDouble()) }

        // the second wall layer
        val layerB = positions.mapArray { BlockPos.ofFloored(it.keyDouble(), yB, it.valueDouble()) }

        // the blocks above the player
        val ceiling = positions.mapArray { BlockPos.ofFloored(it.keyDouble(), yB + 1.0, it.valueDouble()) }

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

    @Suppress("GrazieInspection")
    private fun canEscapeThroughFloorOrCeiling(
        ceiling: Array<BlockPos>,
        floor: Array<BlockPos>
    ): Boolean {
        // Can we escape through the ceiling?
        ceiling.forEach { pos1 ->
            if (!pos1.getState()!!.isSolid && !pos1.up().getState()!!.isSolid) {
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
            if (!pos1.getState()!!.isSolid && !pos1.down().getState()!!.isSolid) {
                return true
            }
        }

        return false
    }

    private fun canEscapeThroughSides(layerA: Array<BlockPos>, layerB: Array<BlockPos>): Boolean {
        // Do we find and escape side?
        layerA.forEachIndexed { index, pos1 ->
            if (!pos1.getState()!!.isSolid && !layerB.elementAt(index).getState()!!.isSolid) {
                return true
            }
        }

        return false
    }

}
