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

package net.ccbluex.liquidbounce.render.gui

import net.ccbluex.liquidbounce.additions.drawCooldownProgress
import net.ccbluex.liquidbounce.additions.drawItemBar
import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.function.Consumer

@Suppress("TooManyFunctions")
object ItemStackListRenderer : EventListener {

    /**
     * @see StatsScreen.SLOT_SPRITE
     */
    private val ID_SINGLE_SLOT = Identifier.withDefaultNamespace("container/slot")

    private val textRenderer = mc.font
    private val planned = ArrayList<ItemStackListRenderState>()

    @JvmStatic
    fun Block.createItemStackForRendering(count: Int): ItemStack {
        val mappedItem = when (this) {
            Blocks.WATER -> Items.WATER_BUCKET
            Blocks.LAVA -> Items.LAVA_BUCKET
            else -> this.asItem()
        }

        return ItemStack(mappedItem, count)
    }

    fun draw(graphics: GuiGraphicsExtractor, state: ItemStackListRenderState, rearrange: Boolean) {
        if (state.stacks.isEmpty() && state.title == null) return

        if (!rearrange) {
            with(graphics) {
                drawNow(state, state.centerX, state.centerY)
            }
            return
        }

        state.updateBounds()
        planned += state
    }

    private fun drawSlotTexture(guiGraphics: GuiGraphicsExtractor, x: Int, y: Int) {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            ID_SINGLE_SLOT,
            x,
            y,
            ITEM_STACK_SLOT_SIZE,
            ITEM_STACK_SLOT_SIZE,
        )
    }

    @Suppress("CognitiveComplexMethod")
    context(graphics: GuiGraphicsExtractor)
    private fun drawNow(
        state: ItemStackListRenderState,
        centerX: Float,
        centerY: Float,
    ) {
        val dimensions = ItemStackListLayout.measureContent(state)

        graphics.pose().withPush {
            val width = dimensions.width
            val height = dimensions.height

            translate(centerX, centerY)
            scale(state.scale, state.scale)
            translate(-width * 0.5F, -height * 0.5F)

            val background = state.background
            if (background is GuiPanel &&
                graphics.drawPanel(width.toFloat(), height.toFloat(), background)
            ) {
                // Elements of a stratum are sorted by pipeline creation order, so pipelines of this mod are drawn
                // after the vanilla item pipelines. Slots and items need a stratum of their own to stay above the
                // background.
                graphics.nextStratum()
            }

            state.title?.let { title ->
                graphics.centeredText(textRenderer, title, width / 2, 0, state.titleColor.argb)
                translate(0F, textRenderer.lineHeight + 2F)
            }

            val onSlots = background is ItemStackListBackground.Slots
            state.stacks.forEachIndexed { i, stack ->
                val leftX = i % state.rowLength * ITEM_STACK_SLOT_SIZE
                val topY = i / state.rowLength * ITEM_STACK_SLOT_SIZE
                if (onSlots) {
                    drawSlotTexture(graphics, leftX, topY)
                }

                val diff = (ITEM_STACK_SLOT_SIZE - ITEM_STACK_ITEM_SIZE) / 2
                with(state.itemStackRenderer) {
                    graphics.drawItemStack(textRenderer, i, stack, leftX + diff, topY + diff)
                }
            }
        }
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(READ_FINAL_STATE) {
        if (planned.isEmpty()) return@handler

        try {
            if (planned.size > 1) {
                GuiOverlapRearranger.rearrange(planned)
            }

            with(it.context) {
                planned.forEach { state ->
                    drawNow(state, state.bounds.xCenter, state.bounds.yCenter)
                }
            }
        } finally {
            planned.clear()
        }
    }

    sealed class BackgroundMode(name: String, override val parent: ModeValueGroup<*>) : Mode(name) {

        /**
         * Snapshot of this mode, rebuilt on the next read after one of its settings changed.
         */
        abstract val background: ItemStackListBackground

        class Rect(parent: ModeValueGroup<*>) : BackgroundMode("Rect", parent) {
            private val invalidate = Consumer<Any?> {
                snapshot = null
            }

            private val fillColor by color("Color", Color4b.DEFAULT_BG_COLOR).onChanged(invalidate)
            private val outlineColor by color("OutlineColor", Color4b.TRANSPARENT).onChanged(invalidate)
            private val margin by float("Margin", DEFAULT_PANEL_MARGIN, 0F..100F).onChanged(invalidate)
            private val radius by float("Radius", DEFAULT_PANEL_RADIUS, 0F..16F).onChanged(invalidate)
            private val gradient by float("Gradient", DEFAULT_PANEL_GRADIENT, 0F..1F).onChanged(invalidate)
            private val glow by float("Glow", DEFAULT_PANEL_GLOW, 0F..1F).onChanged(invalidate)

            private var snapshot: GuiPanel? = null

            // Configured values are trusted only up to the ranges above, an edited config file can hold anything.
            override val background: GuiPanel
                get() = snapshot ?: GuiPanel(
                    fillColor = fillColor,
                    outlineColor = outlineColor,
                    margin = margin,
                    radius = radius,
                    gradient = gradient,
                    glow = glow,
                ).also { snapshot = it }
        }

        class Texture(parent: ModeValueGroup<*>) : BackgroundMode("Texture", parent) {
            override val background get() = ItemStackListBackground.Slots
        }

        companion {
            fun backgroundChoices(parent: ModeValueGroup<*>) = arrayOf(
                Rect(parent),
                Texture(parent),
            )
        }
    }

    fun interface SingleItemStackRenderer {
        fun GuiGraphicsExtractor.drawItemStack(font: Font, index: Int, stack: ItemStack, x: Int, y: Int)

        companion object {

            @JvmField
            val OnlyItem = SingleItemStackRenderer { _, _, stack, x, y ->
                item(stack, x, y)
            }

            @JvmField
            val All = SingleItemStackRenderer { textRenderer, _, stack, x, y ->
                item(stack, x, y)
                itemDecorations(textRenderer, stack, x, y)
            }

            @JvmField
            val ForOtherPlayer = of(drawItemBar = true, drawStackCount = true, drawCooldownProgress = false)

            @JvmStatic
            fun of(
                drawItemBar: Boolean = true,
                drawStackCount: Boolean = true,
                drawCooldownProgress: Boolean = true,
            ): SingleItemStackRenderer {
                return SingleItemStackRenderer { textRenderer, _, stack, x, y ->
                    if (stack.isEmpty) return@SingleItemStackRenderer
                    item(stack, x, y)
                    pose().withPush {
                        if (drawItemBar) drawItemBar(stack, x, y)
                        if (drawStackCount) drawStackCount(textRenderer, stack, x, y, null)
                        if (drawCooldownProgress) drawCooldownProgress(stack, x, y)
                    }
                }
            }
        }
    }
}
