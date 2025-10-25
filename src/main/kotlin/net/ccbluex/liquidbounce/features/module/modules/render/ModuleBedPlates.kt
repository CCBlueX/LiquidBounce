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
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BedStateChangeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.createItemStackForRendering
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.block.bed.isSelfBedChoices
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.function.Predicate

object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER), BedBlockTracker.Subscriber {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII")

    private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))

    override val maxLayers by int("MaxLayers", 5, 1..5).onChanged {
        BedBlockTracker.triggerRescan()
    }
    private val showBed by boolean("ShowBed", true)
    private val textShadow by boolean("TextShadow", true)
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
    private val maxDistance by float("MaxDistance", 256.0f, 128.0f..1280.0f)
    private val maxCount by int("MaxCount", 8, 1..64)
    private val highlightUnbreakable by boolean("HighlightUnbreakable", true)
    private val compact by boolean("Compact", true)
    private val filterMode = choices("FilterMode", 0) {
        arrayOf(FilterMode.Predefined, FilterMode.Custom)
    }
    private val ignoreSelfBed = choices("IgnoreSelfBed", 0, ::isSelfBedChoices)

    private sealed class FilterMode(name: String) : Choice(name), Predicate<Block> {
        final override val parent: ChoiceConfigurable<*>
            get() = filterMode

        object Predefined : FilterMode("Predefined") {
            private val WHITELIST_NON_SOLID: Set<Block> = ReferenceOpenHashSet.of(
                Blocks.LADDER,

                Blocks.WATER,

                Blocks.GLASS,
                Blocks.WHITE_STAINED_GLASS,
                Blocks.ORANGE_STAINED_GLASS,
                Blocks.MAGENTA_STAINED_GLASS,
                Blocks.LIGHT_BLUE_STAINED_GLASS,
                Blocks.YELLOW_STAINED_GLASS,
                Blocks.LIME_STAINED_GLASS,
                Blocks.PINK_STAINED_GLASS,
                Blocks.GRAY_STAINED_GLASS,
                Blocks.LIGHT_GRAY_STAINED_GLASS,
                Blocks.CYAN_STAINED_GLASS,
                Blocks.PURPLE_STAINED_GLASS,
                Blocks.BLUE_STAINED_GLASS,
                Blocks.BROWN_STAINED_GLASS,
                Blocks.GREEN_STAINED_GLASS,
                Blocks.RED_STAINED_GLASS,
                Blocks.BLACK_STAINED_GLASS,
            )

            override fun test(block: Block): Boolean {
                val state = block.defaultState
                return !(state.isAir || !state.isSolidBlock(world, BlockPos.ORIGIN) && block !in WHITELIST_NON_SOLID)
            }
        }

        object Custom : FilterMode("Custom") {
            private val blocks by blocks("Blocks", ReferenceOpenHashSet())
            private val filter by enumChoice("Filter", Filter.BLACKLIST)

            override fun test(block: Block): Boolean {
                return filter(block, blocks)
            }
        }
    }

    private data class BedStateAndDistance(
        @JvmField val bedState: BedState,
        @JvmField var distance: Double,
    ) : Comparable<BedStateAndDistance> {
        override fun compareTo(other: BedStateAndDistance): Int {
            return distance.compareTo(other.distance)
        }
    }

    private val beds = ArrayList<BedStateAndDistance>()

    private fun updateAndSortBeds() {
        val cameraPos = (mc.cameraEntity ?: mc.player ?: return).pos
        beds.forEach {
            it.distance = it.bedState.pos.distanceTo(cameraPos)
        }
        beds.sort()
    }

    @Suppress("unused")
    // Run on render thread because the scanner runs async
    private val bedStateChangeHandler = suspendHandler<BedStateChangeEvent>(Dispatchers.Minecraft) { event ->
        beds.clear()
        beds.ensureCapacity(event.bedStates.size)
        event.bedStates.mapTo(beds) { BedStateAndDistance(it, 0.0) }
        updateAndSortBeds()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        updateAndSortBeds()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        beds.sort()

        var i = 0
        for ((bedState, distance) in beds) {
            if (ignoreSelfBed.activeChoice.isSelfBed(bedState.block, bedState.trackedBlockPos)) {
                continue
            }

            if (distance > maxDistance || i++ > maxCount) {
                break // because list beds are sorted by distance (ASC), so we break at first item out of range
            }

            val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(renderOffset)) ?: continue
            val surrounding = (if (compact) bedState.compactSurroundingBlocks else bedState.surroundingBlocks)
                .filter { filterMode.activeChoice.test(it.block) }

            val blocksAsItemStacks = if (showBed) {
                val list = ArrayList<ItemStack>(surrounding.size + 1) // Add bed itself at first
                list.add(bedState.block.asItem().defaultStack)
                surrounding.mapTo(list) { it.block.createItemStackForRendering(it.count) }
            } else {
                surrounding.map { it.block.createItemStackForRendering(it.count) }
            }

            event.context.drawItemStackList(blocksAsItemStacks)
                .rowLength(Int.MAX_VALUE)
                .scale(scale)
                .center(screenPos)
                .rectBackground(color = backgroundColor.toARGB())
                .itemStackRenderer { textRenderer, index, stack, x, y ->
                    if (index == 0 && showBed) {
                        // bed
                        drawItem(stack, x, y)
                        drawStackCount(textRenderer, stack, x, y, "${distance.toInt()}m")
                    } else {
                        val surroundingBlock = surrounding[if (showBed) index - 1 else index]
                        val defaultState = surroundingBlock.block.defaultState
                        val color =
                            if (highlightUnbreakable && defaultState.isToolRequired
                                && Slots.Hotbar.findSlot { s -> s.isSuitableFor(defaultState) } == null
                            ) {
                                Color4b.RED
                            } else {
                                Color4b.WHITE
                            }.toARGB()

                        drawItem(stack, x, y)
                        val countString = stack.count.toString()
                        matrices.push()
                        matrices.translate(0.0F, 0.0F, 200.0F)
                        // draw layer text
                        if (!compact) {
                            drawText(textRenderer, ROMAN_NUMERALS[surroundingBlock.layer], x, y, color, textShadow)
                        }
                        // drawStackCount, with custom color (copied from DrawContext)
                        drawText(
                            textRenderer,
                            countString,
                            x + 19 - 2 - textRenderer.getWidth(countString),
                            y + 6 + 3,
                            color,
                            textShadow,
                        )
                        matrices.pop()
                    }
                }.draw()
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
