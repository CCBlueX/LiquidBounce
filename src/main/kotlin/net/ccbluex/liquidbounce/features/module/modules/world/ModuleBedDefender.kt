/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.render.BED_BLOCKS
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.IsSelfBedChoice
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.IsSelfBedColorChoice
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.IsSelfBedNoneChoice
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.IsSelfBedSpawnLocationChoice
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.ccbluex.liquidbounce.utils.item.isFullBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.BlockItem
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object ModuleBedDefender : Module("BedDefender", category = Category.WORLD) {

    private val maxLayers by int("MaxLayers", 1, 1..5)

    private val isSelfBedMode = choices<IsSelfBedChoice>("SelfBed", { it.choices[0] }, { arrayOf(
        IsSelfBedNoneChoice(it),
        IsSelfBedColorChoice(it),
        IsSelfBedSpawnLocationChoice(it)
    )})

    private val placer = tree(BlockPlacer("Place", this, Priority.NOT_IMPORTANT, {
        val selected = player.inventory.selectedSlot
        var maxHardness = Float.MIN_VALUE
        var maxCount = 0
        var best: HotbarItemSlot? = null

        HOTBAR_SLOTS.forEach {
            if (!it.itemStack.isFullBlock()) {
                return@forEach
            }

            val hardness = (it.itemStack.item as BlockItem).block.hardness
            // -1 is unbreakable
            if (hardness < maxHardness && hardness != -1f || maxHardness == -1f && hardness != -1f) {
                return@forEach
            }

            // prioritize blocks with a higher hardness
            if (hardness > maxHardness || hardness == -1f && maxHardness != -1f) {
                best = it
                maxHardness = hardness
                return@forEach
            }

            // prioritize stacks with a higher count
            val count = it.itemStack.count
            if (count > maxCount) {
                best = it
                maxCount = count
            }

            // prioritize stacks closer to the selected slot
            val distance1a = (it.hotbarSlot - selected + 9) % 9
            val distance1b = (selected - it.hotbarSlot + 9) % 9
            val distance1 = minOf(distance1a, distance1b)

            val distance2a = (best!!.hotbarSlot - selected + 9) % 9
            val distance2b = (selected - best!!.hotbarSlot + 9) % 9
            val distance2 = minOf(distance2a, distance2b)

            if (distance1 < distance2) {
                best = it
            }
        }

        best
    }, false))

    private val requiresSneak by boolean("RequiresSneak", false)

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        if (!placer.ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        if (!placer.ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        if (requiresSneak && !player.isSneaking) {
            return@handler
        }

        placer.slotFinder() ?: return@handler

        val eyesPos = player.eyes
        val rangeSq = placer.range * placer.range
        val bedBlocks = searchBlocksInCuboid(placer.range + 1, eyesPos) { pos, state ->
            getNearestPoint(eyesPos, Box.enclosing(pos, pos.add(1, 1, 1))).squaredDistanceTo(eyesPos) <= rangeSq
            && (state.block as? BedBlock)?.let {
                bedBlock -> isSelfBedMode.activeChoice.shouldDefend(bedBlock, pos)
            } == true
        }

        if (bedBlocks.isEmpty()) {
            return@handler
        }

        // With [getPlacementPositions] we should be fine with only one bed block
        val (blockPos, state) = bedBlocks.minByOrNull { (blockPos, _) -> blockPos.getSquaredDistance(eyesPos) }
            ?: return@handler

        val playerPosition = player.pos

        val placementPositions = getPlacementPositions(blockPos, state)
            // todo: sort by usefulness instead, currently it just places the block as far as possible
            //   so we prevent placing blocks in front of our face
            .sortedBy { pos -> -pos.getSquaredDistance(playerPosition) }
        if (placementPositions.isEmpty()) {
            return@handler
        }

        ModuleDebug.debugGeometry(this, "PlacementPosition", ModuleDebug.DebugCollection(
            placementPositions.map { pos -> ModuleDebug.DebuggedPoint(pos.toCenterPos(), Color4b.RED.alpha(100)) }
        ))

        placer.update(placementPositions.toSet())
    }

    override fun disable() {
        placer.disable()
    }

    private fun getPlacementPositions(pos: BlockPos, state: BlockState): List<BlockPos> {
        val pos2 = pos.offset(BedBlock.getOppositePartDirection(state))
        if (pos2.getState()?.block !in BED_BLOCKS) {
            return emptyList()
        }

        return (pos.getPlacementPositionsAround() + pos2.getPlacementPositionsAround())
            .distinct()
    }

    private fun BlockPos.getPlacementPositionsAround(): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()

        val layers = Layer.entries
        for ((yOffset, outermostLayer) in (maxLayers downTo 0).withIndex()) {

            // ignore the layer where the block itself is located in
            val innermostLayer = if (yOffset == 0) 1 else 0

            for (currentLayer in outermostLayer downTo innermostLayer) {
                layers[currentLayer].offsets.forEach {
                    positions += this.add(it[0], yOffset, it[1])
                }
            }
        }

        return positions
    }

    // (x, z)
    private enum class Layer(vararg val offsets: IntArray) {

        ZERO(intArrayOf(0, 0)),
        ONE(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)),
        TWO(
            intArrayOf(-2, 0), intArrayOf(2, 0), intArrayOf(0, -2), intArrayOf(0, 2),
            intArrayOf(-1, -1), intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1)
        ),
        THREE(
            intArrayOf(-3, 0), intArrayOf(3, 0), intArrayOf(0, -3), intArrayOf(0, 3),
            intArrayOf(-2, -1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(2, 1),
            intArrayOf(-1, -2), intArrayOf(1, -2), intArrayOf(-1, 2), intArrayOf(1, 2)
        ),
        FOUR(
            intArrayOf(-4, 0), intArrayOf(4, 0), intArrayOf(0, -4), intArrayOf(0, 4),
            intArrayOf(-3, -1), intArrayOf(3, -1), intArrayOf(-3, 1), intArrayOf(3, 1),
            intArrayOf(-1, -3), intArrayOf(1, -3), intArrayOf(-1, 3), intArrayOf(1, 3),
            intArrayOf(-2, -2), intArrayOf(2, -2), intArrayOf(-2, 2), intArrayOf(2, 2)
        ),
        FIVE(
            intArrayOf(-5, 0), intArrayOf(5, 0), intArrayOf(0, -5), intArrayOf(0, 5),
            intArrayOf(-4, -1), intArrayOf(4, -1), intArrayOf(-4, 1), intArrayOf(4, 1),
            intArrayOf(-1, -4), intArrayOf(1, -4), intArrayOf(-1, 4), intArrayOf(1, 4),
            intArrayOf(-3, -2), intArrayOf(3, -2), intArrayOf(-3, 2), intArrayOf(3, 2),
            intArrayOf(-2, -3), intArrayOf(2, -3), intArrayOf(-2, 3), intArrayOf(2, 3)
        )

    }

}
