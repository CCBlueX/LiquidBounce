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

/**
 * Draw a tag for a list of [ItemStack]s.
 *
 * @param centerPos The render position, also the center of the whole tag.
 * @param rowLength The maximum count of stack which can be placed in one row.
 */
@Suppress("LongParameterList")
fun DrawContext.drawItemTags(
    stacks: List<ItemStack>,
    centerPos: Vec3,
    backgroundColor: Int = Int.MIN_VALUE,
    backgroundMargin: Int = 2,
    scale: Float = 1.0F,
    rowLength: Int = 9,
) {
    if (stacks.isEmpty()) return

    val width = ITEM_SIZE * minOf(stacks.size, rowLength)
    val height = ITEM_SIZE * (stacks.size / rowLength + if (stacks.size % rowLength != 0) 1 else 0)

    matrices.push()

    matrices.translate(centerPos.x, centerPos.y, centerPos.z)
    matrices.scale(scale, scale, 1.0F)
    matrices.translate(-width * 0.5F, -height * 0.5F, 0.0F)

    // draw background
    fill(
        -backgroundMargin,
        -backgroundMargin,
        width + backgroundMargin,
        height + backgroundMargin,
        backgroundColor
    )

    // render stacks
    stacks.forEachIndexed { i, stack ->
        if (stack.isEmpty) return@forEachIndexed

        val leftX = i % rowLength * ITEM_SIZE
        val topY = i / rowLength * ITEM_SIZE

        drawItem(stack, leftX, topY)
        drawStackOverlay(mc.textRenderer, stack, leftX, topY)
    }

    matrices.pop()
}

fun DrawContext.drawItemTagsWithSlotTexture(
    stacks: List<ItemStack>,
    centerPos: Vec3,
    scale: Float = 1.0F,
    rowLength: Int = 9,
) {
    if (stacks.isEmpty()) return

    val width = SLOT_SIZE * minOf(stacks.size, rowLength)
    val height = SLOT_SIZE * (stacks.size / rowLength + if (stacks.size % rowLength != 0) 1 else 0)

    matrices.push()

    matrices.translate(centerPos.x, centerPos.y, centerPos.z)
    matrices.scale(scale, scale, 1.0F)
    matrices.translate(-width * 0.5F, -height * 0.5F, 0.0F)

    // render stacks
    stacks.forEachIndexed { i, stack ->
        if (stack.isEmpty) return@forEachIndexed

        val slotLeftX = i % rowLength * SLOT_SIZE
        val slotTopY = i / rowLength * SLOT_SIZE
        val itemLeftX = slotLeftX + (SLOT_SIZE - ITEM_SIZE) / 2
        val itemTopY = slotTopY + (SLOT_SIZE - ITEM_SIZE) / 2

        drawGuiTexture(
            RenderLayer::getGuiTextured,
            ID_SINGLE_SLOT,
            slotLeftX,
            slotTopY,
            SLOT_SIZE,
            SLOT_SIZE,
        )
        drawItem(stack, itemLeftX, itemTopY)
        drawStackOverlay(mc.textRenderer, stack, itemLeftX, itemTopY)
    }

    matrices.pop()
}
