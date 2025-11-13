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

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.LayerAndBlockPos
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquaredEyes
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.searchBedLayer
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.PreferBlockHardness
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.item.asItemSlotComparator
import net.ccbluex.liquidbounce.utils.item.isFullBlock
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.BedBlock
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object ModuleBedDefender : ClientModule("BedDefender", category = Category.WORLD) {

    private val maxLayers by int("MaxLayers", 1, 1..5)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)

    private val placer = tree(BlockPlacer("Place", this, Priority.NOT_IMPORTANT, {
        Slots.OffhandWithHotbar.filter { it.itemStack.isFullBlock() }.minWithOrNull(COMPARATOR_SLOT)
    }, false))

    private val requiresSneak by boolean("RequiresSneak", false)

    private val COMPARATOR_SLOT =
        ComparatorChain(
            PreferBlockHardness.STRONG_FIRST.asItemSlotComparator(),
            PreferStackSize.PREFER_MORE.asItemSlotComparator(),
            HotbarItemSlot.PREFER_NEARBY,
        )

    // Layer(ASC) Center Distance(DESC)
    private val COMPARATOR_PLACEMENT_TARGET =
        Comparator.comparingInt<LayerAndBlockPos> { it.layer }
            .thenComparingDouble {
                -it.blockPos.getCenterDistanceSquaredEyes()
            }

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

        val slotToUse = placer.slotFinder(null) ?: return@handler

        val eyesPos = player.eyePos
        val rangeSq = placer.range * placer.range

        // The bed that need to be defended may be already covered, so we search further
        val bedBlocks = eyesPos.searchBlocksInCuboid(placer.range + maxLayers + 1) { pos, state ->
            val block = state.block
            when {
                block !is BedBlock -> false
                else -> isSelfBedMode.activeChoice.shouldDefend(block, pos)
            }
        }

        // Get the closest bed block
        val (blockPos, state) = bedBlocks.minByOrNull {
            (blockPos, _) -> blockPos.getSquaredDistance(eyesPos)
        } ?: return@handler

        val itemStack = slotToUse.itemStack
        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                listOf(BlockPos.ORIGIN),
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory, considerFacingAwayFaces = placer.wallRange > 0f),
            stackToPlaceWith = itemStack,
            PlayerLocationOnPlacement(position = player.pos),
        )

        val placementPositions = mutableListOf<LayerAndBlockPos>()

        for (target in blockPos.searchBedLayer(state, maxLayers)) {
            val pos = target.blockPos
            if (pos.getCenterDistanceSquaredEyes() > rangeSq) continue
            if (pos.getState()?.isReplaceable != true) continue

            val placementTarget = findBestBlockPlacementTarget(pos, searchOptions) ?: continue
            if (placer.canReach(placementTarget.interactedBlockPos, placementTarget.rotation)) {
                placementPositions.add(target)
            }
        }

        if (placementPositions.isEmpty()) {
            return@handler
        }

        val updatePositions = placementPositions.apply {
            sortWith(COMPARATOR_PLACEMENT_TARGET)
        }

        debugGeometry("BedLayerPositions") {
            ModuleDebug.DebugCollection(
                updatePositions.map {
                    val box = Box(it.blockPos)
                    ModuleDebug.DebuggedBox(box, Color4b.BLUE.with(a = 10))
                }
            )
        }

        debugGeometry("PlacementPositions") {
            ModuleDebug.DebugCollection(
                updatePositions.map {
                    ModuleDebug.DebuggedPoint(it.blockPos.toCenterPos(), Color4b.RED.with(a = 100))
                }
            )
        }

        // Need ordered set (like TreeSet/LinkedHashSet)
        placer.update(
            updatePositions.mapTo(linkedSetOf()) { it.blockPos }
        )
    }

    override fun onDisabled() {
        placer.disable()
    }

}
