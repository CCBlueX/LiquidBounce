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

import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.removeRange
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d
import kotlin.math.sqrt

object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER), BedBlockTracker.Subscriber {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII")

    private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))

    override val maxLayers by int("MaxLayers", 5, 1..5).onChanged {
        BedBlockTracker.triggerRescan()
    }
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
    private val maxDistance by float("MaxDistance", 256.0f, 128.0f..1280.0f)
    private val maxCount by int("MaxCount", 8, 1..64)
    private val highlightUnbreakable by boolean("HighlightUnbreakable", true)
    private val compact by boolean("Compact", true)

    @JvmRecord
    private data class BedStateAndDistance(val bedState: BedState, val distanceSq: Double)

    private val bedStatesWithSquaredDistance by computedOn<GameTickEvent, MutableList<BedStateAndDistance>>(
        initialValue = mutableListOf()
    ) { _, list ->
        val cameraPos = (mc.cameraEntity ?: player).blockPos
        val maxDistanceSquared = maxDistance.sq()
        list.clear()

        BedBlockTracker.iterate().mapTo(list) { (pos, bedState) ->
            BedStateAndDistance(bedState, pos.getSquaredDistance(cameraPos))
        }

        list.removeIf { it.distanceSq > maxDistanceSquared } // filter items out of range
        list.sortBy { it.distanceSq } // order by distance asc
        if (list.size > maxCount) {
            list.removeRange(fromInclusive = maxCount)
        }
        list
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        for ((bedState, distanceSq) in bedStatesWithSquaredDistance) {
            val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(renderOffset)) ?: continue
            val distance = sqrt(distanceSq).toInt()
            val surrounding = if (compact) bedState.compactSurroundingBlocks else bedState.surroundingBlocks

            val blocksAsItemStacks = ArrayList<ItemStack>(surrounding.size + 1) // Add bed itself at first
            blocksAsItemStacks.add(bedState.block.asItem().defaultStack)
            surrounding.mapTo(blocksAsItemStacks) { ItemStack(it.block, it.count) }

            event.context.drawItemStackList(blocksAsItemStacks)
                .rowLength(Int.MAX_VALUE)
                .scale(scale)
                .center(screenPos)
                .rectBackground(color = backgroundColor.toARGB())
                .itemStackRenderer { textRenderer, index, stack, x, y ->
                    if (index == 0) {
                        // bed
                        drawItem(stack, x, y)
                        drawStackCount(textRenderer, stack, x, y, "${distance}m")
                    } else {
                        val surroundingBlock = surrounding[index - 1]
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
                            drawText(textRenderer, ROMAN_NUMERALS[surroundingBlock.layer], x, y, color, true)
                        }
                        // drawStackCount, with custom color
                        drawText(textRenderer, countString, x + 19 - 2 - textRenderer.getWidth(countString), y + 6 + 3, color, true)
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
        bedStatesWithSquaredDistance.clear()
    }
}
