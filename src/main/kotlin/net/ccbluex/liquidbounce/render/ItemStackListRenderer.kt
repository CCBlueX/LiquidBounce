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

package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

private const val SLOT_SIZE = 18
private const val ITEM_SIZE = 16

/**
 * @see net.minecraft.client.gui.screen.StatsScreen.SLOT_TEXTURE
 */
private val ID_SINGLE_SLOT = Identifier.ofVanilla("container/slot")

@Suppress("TooManyFunctions")
class ItemStackListRenderer private constructor(
    private val drawContext: DrawContext,
    private val stacks: List<ItemStack>,
) {
    private var title = ""
    private var titleColor: Int = 0xffffffff.toInt()
    private var centerX = 0.0F
    private var centerY = 0.0F
    private var centerZ = 0.0F
    private var scale = 1.0F
    private var rowLength = 9
    private var backgroundColor = Int.MIN_VALUE
    private var backgroundMargin = 2
    private var useTexture = false
    private var drawStackOverlay = true

    @JvmOverloads
    fun title(title: String, color: Int = this.titleColor) = apply {
        this.title = title
        this.titleColor = color
    }

    fun centerX(centerX: Float) = apply {
        this.centerX = centerX
    }

    fun centerY(centerY: Float) = apply {
        this.centerY = centerY
    }

    fun centerZ(centerZ: Float) = apply {
        this.centerZ = centerZ
    }

    fun center(center: Vec3) = apply {
        this.centerX = center.x
        this.centerY = center.y
        this.centerZ = center.z
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
    fun rectBackground(color: Int, margin: Int = this.backgroundMargin) = apply {
        this.backgroundColor = color
        this.backgroundMargin = margin
    }

    fun textureBackground() = apply {
        this.useTexture = true
    }

    fun background(choice: BackgroundChoice) =
        when (choice) {
            is BackgroundChoice.Rect -> rectBackground(choice.color.toARGB(), choice.margin)
            is BackgroundChoice.Texture -> textureBackground()
        }

    fun drawStackOverlay(drawStackOverlay: Boolean) = apply {
        this.drawStackOverlay = drawStackOverlay
    }

    private fun fillBackground(width: Int, height: Int) {
        drawContext.fill(
            -backgroundMargin,
            -backgroundMargin,
            width + backgroundMargin,
            height + backgroundMargin,
            backgroundColor
        )
    }

    private fun drawSlotTexture(x: Int, y: Int) {
        drawContext.drawGuiTexture(
            RenderLayer::getGuiTextured,
            ID_SINGLE_SLOT,
            x,
            y,
            SLOT_SIZE,
            SLOT_SIZE,
        )
    }

    @Suppress("CognitiveComplexMethod")
    fun draw() {
        if (stacks.isEmpty()) return

        val size = if (this.useTexture) SLOT_SIZE else ITEM_SIZE

        val matrices = drawContext.matrices

        var width = size * minOf(stacks.size, rowLength)
        var height = size * (stacks.size / rowLength + if (stacks.size % rowLength != 0) 1 else 0)

        val textRenderer = mc.textRenderer

        if (title.isNotEmpty()) {
            width = maxOf(width, textRenderer.getWidth(title))
            height += textRenderer.fontHeight + 2
        }

        matrices.push()

        matrices.translate(centerX, centerY, centerZ)
        matrices.scale(scale, scale, 1.0F)
        matrices.translate(-width * 0.5F, -height * 0.5F, 0.0F)

        if (!this.useTexture) {
            fillBackground(width, height)
        }

        if (title.isNotEmpty()) {
            drawContext.drawCenteredTextWithShadow(textRenderer, title, width / 2, 0, titleColor)
            matrices.translate(0F, textRenderer.fontHeight + 2F, 0F)
        }

        // render stacks
        for ((i, stack) in stacks.withIndex()) {
            val leftX = i % rowLength * size
            val topY = i / rowLength * size
            if (this.useTexture) {
                drawSlotTexture(leftX, topY)
            }

            if (stack.isEmpty) continue

            val diff = if (this.useTexture) (SLOT_SIZE - ITEM_SIZE) / 2 else 0

            drawContext.drawItem(stack, leftX + diff, topY + diff)
            if (drawStackOverlay) {
                drawContext.drawStackOverlay(textRenderer, stack, leftX + diff, topY + diff, null)
            }
        }

        matrices.pop()
    }

    companion object {
        @JvmStatic
        @JvmName("create")
        fun DrawContext.drawItemStackList(stacks: List<ItemStack>): ItemStackListRenderer {
            return ItemStackListRenderer(this, stacks)
        }
    }

    sealed class BackgroundChoice(name: String, override val parent: ChoiceConfigurable<*>) : Choice(name) {
        class Rect(parent: ChoiceConfigurable<*>) : BackgroundChoice("Rect", parent) {
            val color by color("Color", Color4b(Int.MIN_VALUE, true))
            val margin by int("Margin", 2, 0..100)
        }

        class Texture(parent: ChoiceConfigurable<*>) : BackgroundChoice("Texture", parent)

        companion object {
            @JvmStatic
            internal fun backgroundChoices(parent: ChoiceConfigurable<*>) = arrayOf(
                Rect(parent),
                Texture(parent),
            )
        }
    }

}

/**
 * Draw a tag for a list of [ItemStack]s.
 *
 * @param centerPos The render position, also the center of the whole tag.
 * @param rowLength The maximum count of stack which can be placed in one row.
 */
@Suppress("LongParameterList")
@JvmOverloads
fun DrawContext.drawItemTags(
    stacks: List<ItemStack>,
    centerPos: Vec3,
    backgroundColor: Int = Int.MIN_VALUE,
    backgroundMargin: Int = 2,
    scale: Float = 1.0F,
    rowLength: Int = 9,
) = drawItemStackList(stacks)
    .centerX(centerPos.x)
    .centerY(centerPos.y)
    .centerZ(centerPos.z)
    .scale(scale)
    .rectBackground(backgroundColor, backgroundMargin)
    .rowLength(rowLength)
    .draw()
