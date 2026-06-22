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

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.liquidbounce.additions.drawCooldownProgress
import net.ccbluex.liquidbounce.additions.drawItemBar
import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/**
 * @see StatsScreen.SLOT_SPRITE
 */
private val ID_SINGLE_SLOT = Identifier.withDefaultNamespace("container/slot")

@Suppress("TooManyFunctions")
object ItemStackListRenderer : EventListener {

    private val textRenderer = mc.font
    private val planned = ArrayList<ItemStackListRenderState>()
    private val overlapRearranger = GuiOverlapRearranger()

    @JvmStatic
    @JvmName("create")
    fun GuiGraphicsExtractor.drawItemStackList(stacks: List<ItemStack>): ItemStackListRenderState {
        return ItemStackListRenderState(this, stacks)
    }

    @JvmStatic
    @JvmName("create")
    fun GuiGraphicsExtractor.drawItemStackList(stacks: Array<ItemStack>): ItemStackListRenderState =
        drawItemStackList(stacks.asList())

    @JvmStatic
    fun Block.createItemStackForRendering(count: Int): ItemStack {
        return ItemStack(block2Item.getOrDefault(this, this.asItem()), count)
    }

    internal fun draw(state: ItemStackListRenderState, rearrange: Boolean) {
        if (state.stacks.isEmpty() && state.title == null) return

        if (!rearrange) {
            drawNow(state, state.bounds.xCenter, state.bounds.yCenter)
            return
        }

        planned += state.copy()
    }

    private fun fillBackground(
        guiGraphics: GuiGraphicsExtractor,
        width: Int,
        height: Int,
        color: Color4b,
        outlineColor: Color4b,
        margin: Float,
    ) {
        guiGraphics.drawQuad(
            -margin,
            -margin,
            width + margin,
            height + margin,
            color,
            outlineColor,
        )
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
    private fun drawNow(
        state: ItemStackListRenderState,
        centerX: Float,
        centerY: Float,
    ) {
        val guiGraphics = state.guiGraphics
        val size = if (state.useTexture) ITEM_STACK_SLOT_SIZE else ITEM_STACK_ITEM_SIZE
        val dimensions = ItemStackListLayout.measureContent(state)

        guiGraphics.pose().withPush {
            val width = dimensions.width
            val height = dimensions.height

            translate(centerX, centerY)
            scale(state.scale, state.scale)
            translate(-width * 0.5F, -height * 0.5F)

            if (!state.useTexture) {
                fillBackground(
                    guiGraphics = guiGraphics,
                    width = width,
                    height = height,
                    color = state.backgroundColor,
                    outlineColor = state.backgroundOutlineColor,
                    margin = state.backgroundMargin,
                )
            }

            state.title?.let { title ->
                guiGraphics.centeredText(textRenderer, title, width / 2, 0, state.titleColor)
                translate(0F, textRenderer.lineHeight + 2F)
            }

            for ((i, stack) in state.stacks.withIndex()) {
                val leftX = i % state.rowLength * size
                val topY = i / state.rowLength * size
                if (state.useTexture) {
                    drawSlotTexture(guiGraphics, leftX, topY)
                }

                val diff = if (state.useTexture) (ITEM_STACK_SLOT_SIZE - ITEM_STACK_ITEM_SIZE) / 2 else 0
                with(state.itemStackRenderer) {
                    guiGraphics.drawItemStack(textRenderer, i, stack, leftX + diff, topY + diff)
                }
            }
        }
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(READ_FINAL_STATE) { _ ->
        if (planned.isEmpty()) return@handler

        try {
            if (planned.size > 1) {
                overlapRearranger.rearrange(planned)
            }

            planned.forEach { state ->
                drawNow(state, state.bounds.xCenter, state.bounds.yCenter)
            }
        } finally {
            planned.clear()
        }
    }

    @JvmStatic
    private val block2Item = Reference2ReferenceOpenHashMap<Block, Item>().apply {
        put(Blocks.WATER, Items.WATER_BUCKET)
        put(Blocks.LAVA, Items.LAVA_BUCKET)
    }

    sealed class BackgroundMode(name: String, override val parent: ModeValueGroup<*>) : Mode(name) {
        class Rect(parent: ModeValueGroup<*>) : BackgroundMode("Rect", parent) {
            val fillColor by color("Color", Color4b.DEFAULT_BG_COLOR)
            val outlineColor by color("OutlineColor", Color4b.TRANSPARENT)
            val margin by float("Margin", 2.0F, 0.0F..100.0F)
        }

        class Texture(parent: ModeValueGroup<*>) : BackgroundMode("Texture", parent)

        companion object {
            @JvmStatic
            internal fun backgroundChoices(parent: ModeValueGroup<*>) = arrayOf(
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
