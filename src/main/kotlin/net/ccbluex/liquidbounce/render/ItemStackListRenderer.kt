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

package net.ccbluex.liquidbounce.render

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.liquidbounce.additions.drawCooldownProgress
import net.ccbluex.liquidbounce.additions.drawItemBar
import net.ccbluex.liquidbounce.additions.drawStackCount
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.joml.Vector2fc
import org.joml.Vector2i
import kotlin.math.abs

/**
 * @see StatsScreen.ItemStatisticsList.SLOT_BG_SIZE
 */
private const val SLOT_SIZE = 18
private const val ITEM_SIZE = GuiRenderer.DEFAULT_ITEM_SIZE

/**
 * @see StatsScreen.SLOT_SPRITE
 */
private val ID_SINGLE_SLOT = Identifier.withDefaultNamespace("container/slot")

@Suppress("TooManyFunctions")
class ItemStackListRenderer private constructor(
    private val drawContext: GuiGraphics,
    private val stacks: List<ItemStack>,
) {
    private var title: Component? = null
    private var titleColor: Int = 0xffffffff.toInt()
    private var centerX = 0F
    private var centerY = 0F
    private var scale = 1.0F
    private var rowLength = 9
    private var backgroundColor = Color4b.DEFAULT_BG_COLOR
    private var backgroundOutlineColor = Color4b.TRANSPARENT
    private var backgroundMargin = 2.0F
    private var useTexture = false
    private var itemStackRenderer = SingleItemStackRenderer.All

    // Unscaled, without margin
    private val dimensions = Vector2i()
    private val textRenderer = mc.font

    @JvmOverloads
    fun title(title: Component?, color: Int = this.titleColor) = apply {
        this.title = title
        this.titleColor = color
    }

    fun centerX(centerX: Float) = apply {
        this.centerX = centerX
    }

    fun centerY(centerY: Float) = apply {
        this.centerY = centerY
    }

    fun center(center: Vector2fc) = apply {
        this.centerX = center.x()
        this.centerY = center.y()
    }

    /**
     * @param rowLength The maximum count of stack which can be placed in one row.
     */
    fun rowLength(rowLength: Int) = apply {
        require(rowLength > 0) { "Row length must not be greater than zero." }
        this.rowLength = rowLength
    }

    fun scale(scale: Float) = apply {
        this.scale = scale
    }

    @JvmOverloads
    fun rectBackground(
        color: Color4b,
        outlineColor: Color4b = Color4b.TRANSPARENT,
        margin: Float = this.backgroundMargin,
    ) = apply {
        this.backgroundColor = color
        this.backgroundOutlineColor = outlineColor
        this.backgroundMargin = margin
        this.useTexture = false
    }

    fun textureBackground() = apply {
        this.useTexture = true
        this.backgroundColor = Color4b.TRANSPARENT
        this.backgroundOutlineColor = Color4b.TRANSPARENT
        this.backgroundMargin = 0F
    }

    fun background(choice: BackgroundMode) =
        when (choice) {
            is BackgroundMode.Rect -> rectBackground(choice.fillColor, choice.outlineColor, choice.margin)
            is BackgroundMode.Texture -> textureBackground()
        }

    fun itemStackRenderer(itemStackRenderer: SingleItemStackRenderer) = apply {
        this.itemStackRenderer = itemStackRenderer
    }

    private fun fillBackground(width: Int, height: Int) {
        drawContext.drawQuad(
            -backgroundMargin,
            -backgroundMargin,
            width + backgroundMargin,
            height + backgroundMargin,
            backgroundColor,
            backgroundOutlineColor,
        )
    }

    private fun drawSlotTexture(x: Int, y: Int) {
        drawContext.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            ID_SINGLE_SLOT,
            x,
            y,
            SLOT_SIZE,
            SLOT_SIZE,
        )
    }

    @Suppress("CognitiveComplexMethod")
    private fun drawNow() {
        if (stacks.isEmpty() && title == null) return

        val size = if (this.useTexture) SLOT_SIZE else ITEM_SIZE

        drawContext.pose().withPush {
            val width = dimensions.x
            val height = dimensions.y

            translate(centerX, centerY)
            scale(scale, scale)
            translate(-width * 0.5F, -height * 0.5F)

            if (!useTexture) {
                fillBackground(width, height)
            }

            title?.let { title ->
                drawContext.drawCenteredString(textRenderer, title, width / 2, 0, titleColor)
                translate(0F, textRenderer.lineHeight + 2F)
            }

            // render stacks
            for ((i, stack) in stacks.withIndex()) {
                val leftX = i % rowLength * size
                val topY = i / rowLength * size
                if (useTexture) {
                    drawSlotTexture(leftX, topY)
                }

                val diff = if (useTexture) (SLOT_SIZE - ITEM_SIZE) / 2 else 0
                with(itemStackRenderer) {
                    drawContext.drawItemStack(textRenderer, i, stack, leftX + diff, topY + diff)
                }
            }
        }
    }

    /**
     * Add this render config to plan or draw immediately.
     * All planned renderer will adjust their position to avoid overlapping.
     * [drawNow] will be called later.
     */
    @JvmOverloads
    fun draw(rearrange: Boolean = false) {
        val size = if (this.useTexture) SLOT_SIZE else ITEM_SIZE
        var width = size * minOf(stacks.size, rowLength)
        var height = size * (stacks.size / rowLength + if (stacks.size % rowLength != 0) 1 else 0)

        title?.let { title ->
            width = maxOf(width, textRenderer.width(title))
            height += textRenderer.lineHeight + (if (stacks.isEmpty()) 0 else 2)
        }

        this.dimensions.set(width, height)

        if (!rearrange) {
            drawNow()
        } else {
            planned += this
        }
    }

    companion object : EventListener {
        private val planned = ArrayList<ItemStackListRenderer>()

        // y -> x
        private val comparator = Comparator<ItemStackListRenderer> { o1, o2 ->
            when {
                o1.centerY != o2.centerY -> o1.centerY.compareTo(o2.centerY)
                else -> o1.centerX.compareTo(o2.centerX)
            }
        }

        private const val MAX_ITER = 100

        /**
         * Calculates overlap rectangles
         */
        @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
        private fun adjustPlannedPositions() {
            var iter = 0
            var moved = false
            while (iter++ < MAX_ITER) {
                for (i in 0 until planned.size) {
                    for (j in i + 1 until planned.size) {
                        val a = planned[i]
                        val b = planned[j]

                        val ax = a.centerX
                        val ay = a.centerY
                        val bx = b.centerX
                        val by = b.centerY
                        val aw = (a.dimensions.x + a.backgroundMargin * 2) * a.scale
                        val ah = (a.dimensions.y + a.backgroundMargin * 2) * a.scale
                        val bw = (b.dimensions.x + b.backgroundMargin * 2) * b.scale
                        val bh = (b.dimensions.y + b.backgroundMargin * 2) * b.scale
                        val dx = (aw + bw) / 2 - abs(ax - bx)
                        val dy = (ah + bh) / 2 - abs(ay - by)
                        if (dx > 0 && dy > 0) {
                            if (dx < dy) {
                                b.centerX = bx + (if (ax < bx) dx else -dx)
                            } else {
                                b.centerY = by + (if (ay < by) dy else -dy)
                            }
                            moved = true
                        }
                    }
                }
                if (!moved) {
                    break
                }
            }
        }

        @Suppress("unused")
        private val overlayRenderHandler = handler<OverlayRenderEvent>(READ_FINAL_STATE) { event ->
            when (planned.size) {
                0 -> return@handler
                1 -> {
                    planned[0].drawNow()
                    planned.clear()
                }

                else -> {
                    planned.sortWith(comparator)
                    adjustPlannedPositions()
                    planned.forEach { it.drawNow() }
                    planned.clear()
                }
            }
        }

        @JvmStatic
        private val block2Item = Reference2ReferenceOpenHashMap<Block, Item>().apply {
            put(Blocks.WATER, Items.WATER_BUCKET)
            put(Blocks.LAVA, Items.LAVA_BUCKET)
        }

        @JvmStatic
        @JvmName("create")
        fun GuiGraphics.drawItemStackList(stacks: List<ItemStack>): ItemStackListRenderer {
            return ItemStackListRenderer(this, stacks)
        }

        @JvmStatic
        @JvmName("create")
        fun GuiGraphics.drawItemStackList(stacks: Array<ItemStack>): ItemStackListRenderer =
            drawItemStackList(stacks.asList())

        @JvmStatic
        fun Block.createItemStackForRendering(count: Int): ItemStack {
            return ItemStack(block2Item.getOrDefault(this, this.asItem()), count)
        }
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
        fun GuiGraphics.drawItemStack(font: Font, index: Int, stack: ItemStack, x: Int, y: Int)

        companion object {

            @JvmField
            val OnlyItem = SingleItemStackRenderer { _, _, stack, x, y ->
                renderItem(stack, x, y)
            }

            @JvmField
            val All = SingleItemStackRenderer { textRenderer, _, stack, x, y ->
                renderItem(stack, x, y)
                renderItemDecorations(textRenderer, stack, x, y)
            }

            @JvmField
            val ForOtherPlayer = of(drawItemBar = true, drawStackCount = true, drawCooldownProgress = false)

            @JvmStatic
            fun of(
                drawItemBar: Boolean = true,
                drawStackCount: Boolean = true,
                drawCooldownProgress: Boolean = true,
            ): SingleItemStackRenderer {
                return SingleItemStackRenderer { textRenderer, index, stack, x, y ->
                    if (stack.isEmpty) return@SingleItemStackRenderer
                    renderItem(stack, x, y)
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
