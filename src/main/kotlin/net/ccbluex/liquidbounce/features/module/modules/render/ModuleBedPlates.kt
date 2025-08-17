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

import it.unimi.dsi.fastutil.doubles.DoubleObjectPair
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.newDrawContext
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.kotlin.removeRange
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ITEM_SIZE: Int = 16
private const val BACKGROUND_PADDING: Int = 2

object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER), BedBlockTracker.Subscriber {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V")

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

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val bedStatesWithSquaredDistance by computedOn<GameTickEvent, MutableList<DoubleObjectPair<BedState>>>(
        initialValue = mutableListOf()
    ) { _, list ->
        val cameraPos = (mc.cameraEntity ?: player).blockPos
        val maxDistanceSquared = maxDistance.sq()
        list.clear()

        BedBlockTracker.iterate().mapTo(list) { (pos, bedState) ->
            DoubleObjectPair.of(pos.getSquaredDistance(cameraPos), bedState)
        }

        list.removeIf { it.firstDouble() > maxDistanceSquared } // filter items out of range
        list.sortBy { it.firstDouble() } // order by distance asc
        if (list.size > maxCount) {
            list.removeRange(fromInclusive = maxCount)
        }
        list
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                bedStatesWithSquaredDistance.forEachWithSelf { (distSq, bedState), i, self ->
                    val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(renderOffset))
                        ?: return@forEachWithSelf
                    val distance = sqrt(distSq)
                    val surrounding = if (compact) bedState.compactSurroundingBlocks else bedState.surroundingBlocks

                    val z = 1000.0F * (self.size - i - 1) / self.size

                    // without padding
                    val rectWidth = ITEM_SIZE * (1 + surrounding.size)
                    val rectHeight = ITEM_SIZE

                    // draw items and background
                    with(newDrawContext()) {
                        with(matrices) {
                            translate(screenPos.x, screenPos.y, z)
                            scale(scale, scale, 1.0F)
                            translate(-0.5F * rectWidth, -0.5F * rectHeight, -1F)

                            fill(
                                -BACKGROUND_PADDING,
                                -BACKGROUND_PADDING,
                                rectWidth + BACKGROUND_PADDING,
                                rectHeight + BACKGROUND_PADDING,
                                backgroundColor.toARGB()
                            )

                            var itemX = 0
                            drawItem(bedState.block.asItem().defaultStack, itemX, 0)
                            surrounding.forEach {
                                itemX += ITEM_SIZE
                                drawItem(it.block.asItem().defaultStack, itemX, 0)
                            }
                        }
                    }

                    // draw texts
                    withMatrixStack {
                        translate(screenPos.x, screenPos.y, z)
                        scale(scale, scale, 1.0F)
                        translate(-0.5F * rectWidth, -0.5F * rectHeight, 150F + 20F)

                        val fontScale = 1.0F / (size * 0.15F)
                        val heightScaled = fontScale * height

                        var topLeftX = 0
                        draw(
                            process("${distance.roundToInt()}m"),
                            0F,
                            rectHeight - heightScaled,
                            shadow = true,
                            scale = fontScale,
                        )
                        commit(buf)
                        surrounding.forEach {
                            topLeftX += ITEM_SIZE

                            val defaultState = it.block.defaultState
                            val color =
                                if (highlightUnbreakable && defaultState.isToolRequired
                                    && Slots.Hotbar.findSlot { s -> s.isSuitableFor(defaultState) } == null
                                ) {
                                    Color4b.RED
                                } else {
                                    Color4b.WHITE
                                }

                            // count
                            val countText = process(it.count.toString(), color)
                            draw(
                                countText,
                                topLeftX + ITEM_SIZE - countText.widthWithShadow * fontScale,
                                rectHeight - heightScaled,
                                shadow = true,
                                scale = fontScale,
                            )
                            commit(buf)

                            if (!compact) {
                                // layer
                                val layerText = process(ROMAN_NUMERALS[it.layer], color)
                                draw(
                                    layerText,
                                    topLeftX.toFloat(),
                                    0F,
                                    shadow = true,
                                    scale = fontScale,
                                )
                                commit(buf)
                            }
                        }
                    }
                }
            }
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
