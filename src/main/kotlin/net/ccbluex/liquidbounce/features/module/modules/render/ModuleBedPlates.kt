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
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BedStateChangeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderer.BackgroundMode.backgroundChoices
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderer.createItemStackForRendering
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderState
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.block.bed.SurroundingBlock
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.addAll
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.joml.Vector2f

object ModuleBedPlates : ClientModule("BedPlates", ModuleCategories.RENDER), BedBlockTracker.Subscriber {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII")

    private val background = modes(this, "Background", 0, ::backgroundChoices)
    private val outline by boolean("Outline", false)

    override val maxLayers by int("MaxLayers", 5, 1..5).onChanged {
        BedBlockTracker.triggerRescan()
    }
    private val showBed by boolean("ShowBed", true)
    private val textShadow by boolean("TextShadow", true)
    private val renderOffset by vec3d("RenderOffset", useLocateButton = false)
    private val scale = curve(
        "Scale",
        mutableListOf(Vector2f(0f, 1f), Vector2f(200f, 1f)),
        xAxis = "Distance" axis 0f..200f,
        yAxis = "Scale" axis 0.25f..4f,
    )
    private val maxCount by int("MaxCount", 8, 1..64)
    private val highlightUnbreakable by boolean("HighlightUnbreakable", true)
    private val compact by boolean("Compact", true)
    private val preventOverlap by boolean("PreventOverlap", true)
    private val filterMode = choices("FilterMode", 0) {
        arrayOf(FilterMode.Predefined, FilterMode.Custom)
    }
    private val ignoreSelfBed = choices("IgnoreSelfBed", 0, ::isSelfBedChoices)
    private val ignoreAdjacent by boolean("IgnoreAdjacent", false)

    private sealed class FilterMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = filterMode

        abstract fun test(block: Block): Boolean

        object Predefined : FilterMode("Predefined") {
            private val WHITELIST_NON_SOLID: Set<Block> = ReferenceOpenHashSet.of(
                Blocks.LADDER,

                Blocks.WATER,

                Blocks.GLASS,
            ).apply {
                addAll(Blocks.STAINED_GLASS)
            }

            override fun test(block: Block): Boolean {
                val state = block.defaultBlockState()
                return !(state.isAir ||
                    !state.isRedstoneConductor(world, BlockPos.ZERO) && block !in WHITELIST_NON_SOLID)
            }
        }

        object Custom : FilterMode("Custom") {
            private val blocks by blocks("Blocks", blockSortedSetOf())
            private val filter by enumChoice("Filter", Filter.BLACKLIST)

            override fun test(block: Block): Boolean {
                return filter(block, blocks)
            }
        }
    }

    private class BedStateRenderState(
        @JvmField val bedState: BedState,
    ) : Comparable<BedStateRenderState> {

        @JvmField var distance: Double = 0.0

        @JvmField val surrounding = ArrayList<SurroundingBlock>()

        @JvmField val itemStacks = ArrayList<ItemStack>()

        // Index mapping of itemStacks: set in updateAndSortBeds together with the list.
        @JvmField var showsBed: Boolean = false

        @JvmField val itemStackListRenderState = ItemStackListRenderState(this.itemStacks)
            .rowLength(Int.MAX_VALUE)
            .itemStackRenderer { textRenderer, index, stack, x, y ->
                val showsBed = this@BedStateRenderState.showsBed
                if (index == 0 && showsBed) {
                    // bed
                    item(stack, x, y)
                    drawStackCount(textRenderer, stack, x, y, "${this@BedStateRenderState.distance.toInt()}m")
                } else {
                    val surroundingBlock = this@BedStateRenderState.surrounding[if (showsBed) index - 1 else index]
                    val defaultState = surroundingBlock.block.defaultBlockState()
                    val color =
                        if (highlightUnbreakable && defaultState.requiresCorrectToolForDrops()
                            && Slots.Hotbar.findSlot { s -> s.isCorrectToolForDrops(defaultState) } == null
                        ) {
                            Color4b.RED
                        } else {
                            Color4b.WHITE
                        }.argb

                    item(stack, x, y)
                    val countString = stack.count.toString()
                    pose().withPush {
                        // draw layer text
                        if (!compact) {
                            text(
                                textRenderer,
                                ROMAN_NUMERALS[surroundingBlock.layer],
                                x,
                                y,
                                color,
                                textShadow,
                            )
                        }
                        // drawStackCount, with custom color (copied from DrawContext)
                        text(
                            textRenderer,
                            countString,
                            x + 19 - 2 - textRenderer.width(countString),
                            y + 6 + 3,
                            color,
                            textShadow,
                        )
                    }
                }
            }

        override fun compareTo(other: BedStateRenderState): Int {
            return distance.compareTo(other.distance)
        }
    }

    private val beds = ArrayList<BedStateRenderState>()

    private fun updateAndSortBeds() {
        val cameraPos = mc.gameRenderer.mainCamera().position()
        for (renderState in beds) {
            val bedState = renderState.bedState
            renderState.distance = bedState.pos.distanceTo(cameraPos)

            val surrounding = renderState.surrounding
            surrounding.clear()
            bedState.surrounding(compact)
                .filterTo(surrounding) { filterMode.activeMode.test(it.block) }

            renderState.showsBed = showBed
            val stacks = renderState.itemStacks
            stacks.clear()
            if (showBed) {
                val bedItemStack = bedState.block.asItem().defaultInstance
                if (surrounding.isEmpty()) {
                    stacks += bedItemStack
                } else {
                    stacks.ensureCapacity(surrounding.size + 1)
                    stacks += bedItemStack // Add bed itself at first
                    surrounding.mapTo(stacks) { it.block.createItemStackForRendering(it.count) }
                }
            } else {
                stacks.ensureCapacity(surrounding.size)
                surrounding.mapTo(stacks) { it.block.createItemStackForRendering(it.count) }
            }
        }
        beds.sort()
    }

    @Suppress("unused")
    // Run on render thread because the scanner runs async
    private val bedStateChangeHandler = handler<BedStateChangeEvent> { event ->
        mc.execute {
            if (!running) return@execute

            beds.clear()
            beds.ensureCapacity(event.bedStates.size)
            event.bedStates.mapTo(beds, ::BedStateRenderState)
            updateAndSortBeds()
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        updateAndSortBeds()
    }

    private fun isAdjacentAndNotEquals(pos1: BlockPos, pos2: BlockPos): Boolean {
        return pos1 != pos2 && pos1.distManhattan(pos2) <= 1
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        beds.sort()

        var i = 0
        for (state in beds) {
            if (i >= maxCount) {
                break
            }

            val bedState = state.bedState
            val currPos = bedState.trackedBlockPos
            val scale = scale.transform(state.distance.toFloat())

            if (scale < 0.01f ||
                ignoreSelfBed.activeMode.isSelfBed(bedState.block, currPos) ||
                ignoreAdjacent && beds.any { isAdjacentAndNotEquals(it.bedState.trackedBlockPos, currPos) }
            ) {
                continue
            }

            val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(renderOffset)) ?: continue

            val outlineColor = if (outline) {
                Color4b.fullAlpha(bedState.block.color.mapColor.col)
            } else {
                null
            }

            state.itemStackListRenderState
                .scale(scale)
                .centerX(screenPos.x)
                .centerY(screenPos.y)
                .background(background.activeMode, outlineColor)
                .draw(event.context, preventOverlap)

            i++
        }
    }

    override fun onEnabled() {
        BedBlockTracker.subscribe(this)
    }

    override fun onDisabled() {
        BedBlockTracker.unsubscribe(this)
        beds.clear()
    }
}
