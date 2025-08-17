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

import it.unimi.dsi.fastutil.ints.IntObjectPair
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.world.fucker.isSelfBedChoices
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.searchBedLayer
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isFullBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.minecraft.block.BedBlock
import net.minecraft.block.DoubleBlockProperties
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.BlockItem
import net.minecraft.util.math.BlockPos

object ModuleBedDefender : ClientModule("BedDefender", category = Category.WORLD) {

    private val maxLayers by int("MaxLayers", 1, 1..5)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)

    private val placer = tree(BlockPlacer("Place", this, Priority.NOT_IMPORTANT, {
        val selected = player.inventory.selectedSlot
        var maxHardness = Float.MIN_VALUE
        var maxCount = 0
        var best: HotbarItemSlot? = null

        Slots.OffhandWithHotbar.forEach {
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
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (!placer.ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        if (!placer.ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        if (requiresSneak && !player.isSneaking) {
            return@handler
        }

        placer.slotFinder(null) ?: return@handler

        val eyesPos = player.eyePos
        val rangeSq = placer.range * placer.range

        // The bed that need to be defended may be already covered, so we search further
        val bedBlocks = eyesPos.searchBlocksInCuboid(placer.range + maxLayers + 1) { pos, state ->
            val block = state.block
            when {
                block !is BedBlock -> false
                BedBlock.getBedPart(state) != DoubleBlockProperties.Type.FIRST -> false
                else -> isSelfBedMode.activeChoice.shouldDefend(block, pos)
            }
        }

        // Get the closest bed block
        val (blockPos, state) = bedBlocks.minByOrNull {
            (blockPos, _) -> blockPos.getSquaredDistance(eyesPos)
        } ?: return@handler

        val placementPositions = blockPos.searchBedLayer(state, maxLayers).filter { (_, pos) ->
            pos.toCenterPos().squaredDistanceTo(eyesPos) <= rangeSq
        }

        if (placementPositions.none()) {
            return@handler
        }

        val updatePositions = placementPositions.toMutableList().apply {
            // Layer(ASC) Center Distance(DESC)
            sortWith(
                Comparator.comparingInt(IntObjectPair<BlockPos>::keyInt)
                    .thenComparingDouble { -it.value().toCenterPos().squaredDistanceTo(eyesPos) }
            )
        }

        debugGeometry("PlacementPosition") {
            ModuleDebug.DebugCollection(
                updatePositions.map { (_, pos) ->
                    ModuleDebug.DebuggedPoint(pos.toCenterPos(), Color4b.RED.with(a = 100))
                }
            )
        }

        // Need ordered set (like TreeSet/LinkedHashSet)
        placer.update(updatePositions.mapTo(linkedSetOf()) { it.value() })
    }

    override fun onDisabled() {
        placer.disable()
    }

}
