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
package net.ccbluex.liquidbounce.features.module.modules.world

import it.unimi.dsi.fastutil.ints.IntLongPair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.searchBedLayer
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isFullBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.BedBlock

object ModuleBedDefender : ClientModule("BedDefender", category = ModuleCategories.WORLD) {

    private val maxLayers by int("MaxLayers", 1, 1..5)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)

    private fun blockHardness(slot: HotbarItemSlot): Float =
        (slot.itemStack.item as BlockItem).block.defaultDestroyTime()

    private val blockSlotComparator =
        compareByDescending<HotbarItemSlot> { blockHardness(it) == -1f }
            .thenByDescending { blockHardness(it) }
            .then(ItemSlot.PREFER_MORE_ITEM)
            .then(HotbarItemSlot.PREFER_NEARBY)

    private fun findBestBlockSlot(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar
            .filter { it.itemStack.isFullBlock() }
            .minWithOrNull(blockSlotComparator)
    }

    private val placer = tree(BlockPlacer("Place", this, Priority.NOT_IMPORTANT, {
        findBestBlockSlot()
    }, false))

    private val requiresSneak by boolean("RequiresSneak", false)

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (!placer.ignoreOpenInventory && mc.screen is AbstractContainerScreen<*>) {
            return@handler
        }

        if (!placer.ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        if (requiresSneak && !player.isShiftKeyDown) {
            return@handler
        }

        placer.slotFinder(null) ?: return@handler

        val eyesPos = player.eyePosition
        val rangeSq = placer.range * placer.range

        // The bed that need to be defended may be already covered, so we search further
        val bedBlocks = eyesPos.searchBlocksInCuboid(placer.range + maxLayers + 1) { pos, state ->
            val block = state.block
            when {
                block !is BedBlock -> false
                else -> isSelfBedMode.activeMode.shouldDefend(block, pos)
            }
        }

        // Get the closest bed block
        val (blockPos, state) = bedBlocks.minByOrNull {
            (blockPos, _) -> blockPos.distToCenterSqr(eyesPos)
        } ?: return@handler

        val mutable = BlockPos.MutableBlockPos()
        val placementPositions = blockPos.searchBedLayer(state, maxLayers)
            .filterTo(mutableListOf()) { (_, pos) ->
                mutable.set(pos).center.distanceToSqr(eyesPos) <= rangeSq
            }

        if (placementPositions.isEmpty()) {
            return@handler
        }

        val updatePositions = placementPositions.apply {
            // Layer(ASC) Center Distance(DESC)
            sortWith(
                Comparator.comparingInt<IntLongPair> { it.leftInt() }
                    .thenComparingDouble {
                        -mutable.set(it.rightLong()).distToCenterSqr(eyesPos)
                    }
            )
        }

        debugGeometry("PlacementPosition") {
            ModuleDebug.DebugCollection(
                updatePositions.map { (_, pos) ->
                    ModuleDebug.DebuggedPoint(mutable.set(pos).center, Color4b.RED.with(a = 100))
                }
            )
        }

        // Need ordered set (like TreeSet/LinkedHashSet)
        placer.update(
            updatePositions.mapTo(linkedSetOf()) {
                BlockPos.of(it.rightLong())
            }
        )
    }

    override fun onDisabled() {
        placer.disable()
    }

}
