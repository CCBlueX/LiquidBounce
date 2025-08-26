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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.placer.placeInstantOnBlockUpdate
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.range
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import kotlin.math.max
import kotlin.math.min

/**
 * Module BlockTrap
 *
 * Traps enemies in blocks.
 *
 * @author ccetl
 */
@Suppress("MagicNumber")
object ModuleBlockTrap : ClientModule("BlockTrap", Category.WORLD) {

    private val doublePlace by multiEnumChoice<DoublePlace>("DoublePlace")

    private val placeAt by multiEnumChoice("PlaceAt", PlaceAt.entries)

    /**
     * Replaces broken blocks instantly.
     *
     * Requires the rotation mode "None" in the block placer.
     */
    private val instant by boolean("Instant", true)

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", hashSetOf())
    private val placePriority by enumChoice("PlacePriority", PlacePriority.FURTHEST)
    private val placer = tree(BlockPlacer("Place", this, Priority.NORMAL, { filter.getSlot(blocks) }))
    private val targetTracker = tree(TargetTracker(
        TargetPriority.DIRECTION,
        FloatValueProvider("Range", 4f, 1f..6f)
    ))

    private val targetRenderer = tree(WorldTargetRenderer(this))

    override fun onDisabled() {
        placer.disable()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        targetTracker.selectFirst()
        val target = targetTracker.target ?: run {
            if (!placer.isDone()) {
                placer.clear()
            }

            return@handler
        }

        val plan = findTrapPlan(target)
        placer.update(plan.sortedWith(placePriority.comparator).toSet())
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            targetRenderer.render(this, target, it.partialTicks)
        }
    }

    @Suppress("unused")
    private val blockUpdateHandler = handler<PacketEvent> {
        if (!instant) {
            return@handler
        }

        placer.placeInstantOnBlockUpdate(it)
    }

    @Suppress("ComplexCondition")
    private fun findTrapPlan(target: Entity): Set<BlockPos> {
        var highestY = Int.MIN_VALUE
        var lowestY = Int.MAX_VALUE

        // step one: find all block positions to around the bounding box
        val boundingBox = target.boundingBox
        val result = mutableSetOf<BlockPos>()
        val xRange = MathHelper.floor(boundingBox.minX - 1.0)..MathHelper.floor(boundingBox.maxX + 1.0)
        val yRange = MathHelper.floor(boundingBox.minY - 1.0)..MathHelper.floor(boundingBox.maxY + 1.0)
        val zRange = MathHelper.floor(boundingBox.minZ - 1.0)..MathHelper.floor(boundingBox.maxZ + 1.0)

        // ranges used to check if a position is in the bounding box
        val bbXRange = MathHelper.floor(boundingBox.minX)..MathHelper.floor(boundingBox.maxX)
        val bbYRange = MathHelper.floor(boundingBox.minY)..MathHelper.floor(boundingBox.maxY)
        val bbZRange = MathHelper.floor(boundingBox.minZ)..MathHelper.floor(boundingBox.maxZ)

        range(xRange step 1, yRange step 1, zRange step 1) { x, y, z ->
            // continue if the position is in the box or outside on two or more axes
            val inBb = x in bbXRange && y in bbYRange && z in bbZRange
            val onlyZIn = x !in bbXRange && y !in bbYRange
            val onlyXIn = y !in bbYRange && z !in bbZRange
            val onlyYIn = z !in bbZRange && x !in bbXRange
            if (inBb || onlyZIn || onlyXIn || onlyYIn) {
                return@range
            }

            val blockPos = BlockPos(x, y, z)
            result += blockPos
            highestY = max(highestY, blockPos.y)
            lowestY = min(lowestY, blockPos.y)
        }

        return tweakPlan(result, lowestY, highestY)
    }

    private fun tweakPlan(currentPlan: Set<BlockPos>, lowestY: Int, highestY: Int): Set<BlockPos> {
        // step two tweaking with options
        val shouldFilterLegsOut = PlaceAt.LEGS !in placeAt // && highestY - lowestY > 3
        val shouldFilterFloorOut = PlaceAt.FLOOR !in placeAt
        val filteredList = currentPlan.filterNot {
            shouldFilterLegsOut && it.y == lowestY + 1 || shouldFilterFloorOut && it.y == lowestY
        }

        val additions = mutableListOf<BlockPos>()
        filteredList.forEach { pos ->
            when {
                DoublePlace.BELOW in doublePlace
                    && PlaceAt.FLOOR in placeAt
                    && pos.y == lowestY -> additions.add(pos.down())

                DoublePlace.ABOVE in doublePlace
                    && pos.y == highestY -> additions.add(pos.up())
            }
        }

        return (filteredList + additions).toSet()
    }

    /**
     * Determines how the blocks are added to the placement queue.
     */
    @Suppress("unused")
    private enum class PlacePriority(
        override val choiceName: String,
        val comparator: Comparator<BlockPos>
    ) : NamedChoice {
        CLOSEST("Closest", compareBy { it.getSquaredDistance(player.pos) }),
        FURTHEST("Furthest", compareByDescending { it.getSquaredDistance(player.pos) }),
        HIGHEST("Highest", compareByDescending { it.y }),
        LOWEST("Lowest", compareBy { it.y })
    }

    private enum class PlaceAt(
        override val choiceName: String
    ) : NamedChoice {
        /**
         * Allows placing crystals next to their legs and keep them at the spot when disabled.
         */
        LEGS("Legs"),

        /**
         * Allows placing crystals next to floor and keep them at the spot when disabled.
         */
        FLOOR("Floor")
    }

    private enum class DoublePlace(
        override val choiceName: String
    ) : NamedChoice {
        /**
         * Places two blocks above the target's head so that they can't mine the block and at the same time tower up to
         * escape.
         */
        ABOVE("Above"),

        /**
         * Places two layers below the target so they can't mine the block below them and possible fall down.
         *
         * Requires [PlaceAt.FLOOR] to be enabled.
         */
        BELOW("Below")
    }
}
