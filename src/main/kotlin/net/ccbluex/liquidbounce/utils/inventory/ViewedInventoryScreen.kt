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
package net.ccbluex.liquidbounce.utils.inventory

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen.BACKGROUND_TEXTURE
import net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

class ViewedInventoryScreen(private val player: () -> PlayerEntity?) : Screen(Text.empty()) {

    val handler: PlayerScreenHandler?
        get() = player()?.playerScreenHandler

    private val backgroundWidth: Int = 176
    private val backgroundHeight: Int = 166
    private var x: Int = (width - backgroundWidth) / 2
    private var y: Int = (height - backgroundHeight) / 2

    override fun init() {
        x = (width - backgroundWidth) / 2
        y = (height - backgroundHeight) / 2
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val handler = handler ?: return
        RenderSystem.disableDepthTest()
        context.matrices.push()
        context.matrices.translate(x.toFloat(), y.toFloat(), 0.0f)
        var hoveredSlot: Slot? = null

        for (slot in handler.slots) {
            if (slot.isEnabled) {
                drawSlot(context, slot)
            }

            if (isPointOverSlot(slot, mouseX.toDouble(), mouseY.toDouble()) && slot.isEnabled) {
                hoveredSlot = slot
                if (slot.canBeHighlighted()) {
                    // draw slot highlight
                    context.fillGradient(
                        RenderLayer.getGuiOverlay(),
                        slot.x, slot.y, slot.x + 16, slot.y + 16,
                        -2130706433, -2130706433, 0
                    )
                }
            }
        }

        val cursorStack = handler.cursorStack
        if (!cursorStack.isEmpty) {
            drawItem(context, cursorStack, mouseX - x - 8, mouseY - y - 8)
        }

        context.matrices.pop()
        RenderSystem.enableDepthTest()

        if (cursorStack.isEmpty && hoveredSlot != null && hoveredSlot.hasStack()) {
            val hoveredItemStack = hoveredSlot.stack
            context.drawTooltip(
                textRenderer, getTooltipFromItem(mc, hoveredItemStack),
                hoveredItemStack.tooltipData, mouseX, mouseY
            )
        }
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderInGameBackground(context)
        drawBackground(context, mouseX, mouseY)
    }

    private fun drawItem(context: DrawContext, stack: ItemStack, x: Int, y: Int) {
        context.matrices.push()
        context.matrices.translate(0f, 0f, 232f)
        context.drawItem(stack, x, y)
        context.drawStackOverlay(textRenderer, stack, x, y, null)
        context.matrices.pop()
    }

    private fun drawBackground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND_TEXTURE, x, y,
            0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
        player()?.let { player ->
            drawEntity(
                context, x + 26, y + 8, x + 75, y + 78,
                30, 0.0625f, mouseX.toFloat(), mouseY.toFloat(), player
            )
        }
    }

    private fun drawSlot(context: DrawContext, slot: Slot) {
        var spriteDrawn = false

        context.matrices.push()
        context.matrices.translate(0f, 0f, 100f)
        if (slot.stack.isEmpty && slot.isEnabled) {
            val identifier = slot.backgroundSprite
            if (identifier != null) {
                context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, slot.x, slot.y, 16, 16);
                spriteDrawn = true
            }
        }

        if (!spriteDrawn) {
            val seed = slot.x + slot.y * backgroundWidth
            if (slot.disablesDynamicDisplay()) {
                context.drawItemWithoutEntity(slot.stack, slot.x, slot.y, seed)
            } else {
                context.drawItem(slot.stack, slot.x, slot.y, seed)
            }

            context.drawStackOverlay(textRenderer, slot.stack, slot.x, slot.y, null)
        }

        context.matrices.pop()
    }

    private fun isPointOverSlot(slot: Slot, pointX: Double, pointY: Double): Boolean {
        val width = 16
        val height = 16
        val pX = pointX - x
        val pY = pointY - y
        return pX >= slot.x - 1 && pX < slot.x + width + 1
            && pY >= slot.y - 1 && pY < slot.y + height + 1
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        super.keyPressed(keyCode, scanCode, modifiers)

        if (mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            close()
        }

        return true
    }

    override fun shouldPause() = false

    override fun tick() {
        if (handler == null) {
            close()
        }
    }
}
